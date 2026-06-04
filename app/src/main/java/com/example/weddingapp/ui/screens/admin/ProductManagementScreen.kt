package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.weddingapp.data.model.ProductItem
import com.example.weddingapp.data.repository.CategoryRepository
import com.example.weddingapp.data.repository.ProductRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableStateOf("all") }

    val products by ProductRepository.getProducts(selectedCategoryId).collectAsState(initial = emptyList())
    var productToDelete by remember { mutableStateOf<ProductItem?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Quản lý sản phẩm", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToAdd) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm mới", tint = Color(0xFFD4AF37))
                        }
                    }
                )

                // --- THANH CHỌN DANH MỤC ---
                val selectedTabIndex = when (selectedCategoryId) {
                    "all" -> 0
                    else -> {
                        val index = categories.indexOfFirst { it.id == selectedCategoryId }
                        if (index != -1) index + 1 else 0
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = Color.White,
                    contentColor = Color(0xFFD4AF37),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedCategoryId == "all",
                        onClick = { selectedCategoryId = "all" },
                        text = { Text("Tất cả", fontSize = 14.sp) }
                    )
                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            text = { Text(category.name, fontSize = 14.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Không có sản phẩm nào trong danh mục này", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val catName = remember(product.categoryId, categories) {
                        categories.find { it.id == product.categoryId }?.name ?: "Đang tải..."
                    }

                    AdminProductItem(
                        product = product,
                        categoryName = catName,
                        onEdit = { onNavigateToEdit(product.id) },
                        onDelete = { productToDelete = product }
                    )
                }
            }
        }

        // Dialog xóa sản phẩm
        productToDelete?.let { product ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc muốn xóa '${product.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        ProductRepository.deleteProduct(product.id) {
                            productToDelete = null
                        }
                    }) { Text("XÓA", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) { Text("HỦY") }
                }
            )
        }
    }
}


@Composable
fun AdminProductItem(
    product: ProductItem,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                Text("${formatPrice(product.price)} VNĐ", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    color = Color(0xFFF0F0F0),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE91E63))
                }
            }
        }
    }
}

fun formatPrice(price: String): String {
    return try {
        val amount = price.toLong()
        val formatter = java.text.DecimalFormat("#,###")
        formatter.format(amount).replace(",", ".")
    } catch (e: Exception) {
        price
    }
}
