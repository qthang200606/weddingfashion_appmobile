package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.ProductItem
import com.example.weddingapp.data.repository.CategoryRepository
import com.example.weddingapp.data.repository.ProductRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementScreen(onBack: () -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    // Lấy toàn bộ sản phẩm
    val allProducts by ProductRepository.getProducts("all").collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showLowStockOnly by remember { mutableStateOf(false) } 
    var productToUpdate by remember { mutableStateOf<ProductItem?>(null) }

    // Logic lọc sản phẩm
    val filteredProducts = allProducts.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true)
        val isLowStock = if (showLowStockOnly) product.stock < 5 else true
        matchesSearch && isLowStock
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Quản Lý Tồn Kho", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                )
                // Thanh tìm kiếm
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm tên sản phẩm...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showLowStockOnly,
                        onClick = { showLowStockOnly = !showLowStockOnly },
                        label = { Text("Sắp hết hàng (< 5)") },
                        leadingIcon = if (showLowStockOnly) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    ) { padding ->
        if (filteredProducts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy sản phẩm nào", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    InventoryItemCard(
                        product = product,
                        onUpdateStock = { productToUpdate = product }
                    )
                }
            }
        }

        // Dialog cập nhật số lượng
        productToUpdate?.let { product ->
            StockUpdateDialog(
                product = product,
                onDismiss = { productToUpdate = null },
                onConfirm = { newStock ->
                    ProductRepository.updateStock(product.id, newStock)
                    productToUpdate = null
                }
            )
        }
    }
}

@Composable
fun InventoryItemCard(product: ProductItem, onUpdateStock: () -> Unit) {
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
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                // Hiển thị trạng thái kho
                val statusText = when {
                    product.stock <= 0 -> "Hết hàng"
                    product.stock < 5 -> "Sắp hết: ${product.stock}"
                    else -> "Đang còn: ${product.stock}"
                }
                val statusColor = when {
                    product.stock <= 0 -> Color.Red
                    product.stock < 5 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }

                Text(text = statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onUpdateStock,
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
            ) {
                Text("Nhập Kho", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StockUpdateDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var stockInput by remember { mutableStateOf(product.stock.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập thêm kho") },
        text = {
            Column {
                Text("Sản phẩm: ${product.name}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) stockInput = it },
                    label = { Text("Tổng số lượng tồn kho") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(stockInput.toIntOrNull() ?: 0) }) {
                Text("XÁC NHẬN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("HỦY") }
        }
    )
}
