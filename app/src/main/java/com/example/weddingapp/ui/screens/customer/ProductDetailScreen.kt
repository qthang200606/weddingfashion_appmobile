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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.CartItem
import com.example.weddingapp.data.model.Review
import com.example.weddingapp.data.repository.CartRepository
import com.example.weddingapp.data.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // 1. Lấy dữ liệu sản phẩm theo ID từ Repository
    val productState = ProductRepository.getProductById(productId).collectAsState(initial = null)
    val product = productState.value

    // 2. Truy vấn dữ liệu tên danh mục từ Database (Categories Collection) bằng categoryId
    var categoryName by remember { mutableStateOf("") }
    LaunchedEffect(product?.categoryId) {
        product?.categoryId?.let { catId ->
            if (catId.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("categories")
                    .document(catId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Lấy trường 'name' bên trong tài liệu danh mục ("vest", "váy cưới",...)
                            categoryName = document.getString("name") ?: ""
                        }
                    }
            }
        }
    }

    // 3. Lấy danh sách Review từ khách hàng
    val reviewsState = remember(productId) {
        FirebaseFirestore.getInstance()
            .collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Review::class.java)
            }
    }.collectAsState(initial = emptyList())

    val reviews = reviewsState.value

    // Tính điểm đánh giá trung bình
    val avgRating = if (reviews.isNotEmpty()) {
        reviews.map { it.rating.toDouble() }.average().toFloat()
    } else 0f

    var selectedSize by remember { mutableStateOf("M") }
    var quantity by remember { mutableIntStateOf(1) }

    // Trạng thái điều khiển việc ẩn/hiện hộp thoại bảng size
    var showSizeChartDialog by remember { mutableStateOf(false) }

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
                    // --- Ảnh sản phẩm ---
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

                    // --- Thông tin chi tiết sản phẩm ---
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(product.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                        // Tóm tắt đánh giá sao
                        RatingSummarySection(avgRating, reviews.size)

                        Text(
                            text = "${formatPriceDetail(product.price)} VNĐ",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = if (isOutOfStock) Color.Gray else Color(0xFFB8860B),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFF1F1F1))

                        // --- Chọn kích cỡ & Nút mở bảng quy đổi size thông minh ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kích cỡ", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // Nút Hướng dẫn chọn size
                            Row(
                                modifier = Modifier.clickable { showSizeChartDialog = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Hướng dẫn chọn size",
                                    fontSize = 14.sp,
                                    color = Color(0xFFD4AF37),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            listOf("S", "M", "L", "XL").forEach { size ->
                                SizeChip(size, selectedSize == size) { selectedSize = size }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // --- Chọn Số lượng ---
                        Text("Số lượng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        QuantitySelector(
                            quantity = quantity,
                            onIncrement = { if (quantity < maxStock) quantity++ },
                            onDecrement = { if (quantity > 1) quantity-- },
                            isEnabled = !isOutOfStock
                        )

                        // --- Mô tả sản phẩm ---
                        Text("Mô tả sản phẩm", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
                        Text(product.description, color = Color.DarkGray, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 8.dp, color = Color(0xFFF8F8F8))

                        // --- Danh sách nhận xét từ khách hàng ---
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

                // --- Thanh chức năng nút bấm phía dưới đáy màn hình ---
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

                // HIỂN THỊ HỘP THOẠI POPUP QUY ĐỔI SỐ ĐO DỰA TRÊN TÊN DANH MỤC LẤY TỪ FIRESTORE
                if (showSizeChartDialog) {
                    // Kiểm tra xem trường name của danh mục lấy từ DB có chứa từ khóa "vest" hay không
                    val isVestCategory = categoryName.contains("vest", ignoreCase = true)

                    SizeChartDialog(
                        isVest = isVestCategory,
                        onDismiss = { showSizeChartDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SizeChartDialog(
    isVest: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = if (isVest) "BẢNG QUY ĐỔI SIZE VEST NAM" else "BẢNG SỐ ĐO VÁY CƯỚI CAO CẤP",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xFF4A4A4A)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tiêu đề các cột dữ liệu phân bổ theo loại trang phục danh mục
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                    if (isVest) {
                        Text("Chiều cao", Modifier.weight(2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                        Text("Cân nặng", Modifier.weight(2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                    } else {
                        Text("Vòng ngực", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                        Text("Vòng eo", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                        Text("Cân nặng", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

                if (isVest) {
                    // DỮ LIỆU SỐ ĐO DANH CHO DANH MỤC VEST NAM
                    SizeDataRow(size = "S", col1 = "1m60 - 1m67", col2 = "50 - 56 kg", isVest = true)
                    SizeDataRow(size = "M", col1 = "1m68 - 1m73", col2 = "57 - 64 kg", isVest = true)
                    SizeDataRow(size = "L", col1 = "1m74 - 1m78", col2 = "65 - 71 kg", isVest = true)
                    SizeDataRow(size = "XL", col1 = "1m79 - 1m85", col2 = "72 - 85 kg", isVest = true)
                } else {
                    // DỮ LIỆU SỐ ĐO CHI TIẾT DÀNH CHO CÔ DÂU (MẶC ĐỊNH CHO VÁY CƯỚI/CÁC LOẠI KHÁC)
                    SizeDataRow(size = "S", col1 = "80 - 84 cm", col2 = "60 - 64 cm", col3 = "42 - 48 kg", isVest = false)
                    SizeDataRow(size = "M", col1 = "85 - 89 cm", col2 = "65 - 69 cm", col3 = "49 - 54 kg", isVest = false)
                    SizeDataRow(size = "L", col1 = "90 - 94 cm", col2 = "70 - 74 cm", col3 = "55 - 60 kg", isVest = false)
                    SizeDataRow(size = "XL", col1 = "95 - 100 cm", col2 = "75 - 80 cm", col3 = "61 - 70 kg", isVest = false)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isVest)
                        "* Lưu ý: Đối với phom vest ôm, nếu thông số cơ thể nằm giữa 2 size, quý khách nên ưu tiên lựa chọn size lớn hơn để mặc thoải mái."
                    else
                        "* Lưu ý: Váy cưới cao cấp thường sở hữu phom corset siết eo cùng dây thắt tùy chỉnh phía sau, giúp cô dâu dễ dàng co giãn ôm khít vòng eo từ 2-4cm.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun SizeDataRow(size: String, col1: String, col2: String, col3: String = "", isVest: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(size, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFFB8860B), textAlign = TextAlign.Center, fontSize = 14.sp)
        Text(col1, Modifier.weight(if (isVest) 2f else 1.5f), color = Color.DarkGray, textAlign = TextAlign.Center, fontSize = 13.sp)
        Text(col2, Modifier.weight(if (isVest) 2f else 1.5f), color = Color.DarkGray, textAlign = TextAlign.Center, fontSize = 13.sp)
        if (!isVest) {
            Text(col3, Modifier.weight(1.8f), color = Color.DarkGray, textAlign = TextAlign.Center, fontSize = 13.sp)
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