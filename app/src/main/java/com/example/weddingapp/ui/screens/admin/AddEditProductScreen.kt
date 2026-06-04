package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.weddingapp.data.repository.ProductRepository
import com.example.weddingapp.data.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(onBack: () -> Unit, productId: String? = null) {
    // 1. Trạng thái Form sản phẩm
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    // 2. Trạng thái Danh mục
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("Chọn danh mục") }

    var expandedMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val isEditMode = productId != null

    // 3. Load dữ liệu cũ nếu ở chế độ Sửa
    LaunchedEffect(productId, categories) {
        if (productId != null && categories.isNotEmpty()) {
            isLoading = true
            FirebaseFirestore.getInstance().collection("products").document(productId)
                .get()
                .addOnSuccessListener { doc ->
                    val p = doc.toObject(ProductItem::class.java)
                    p?.let {
                        name = it.name
                        price = it.price
                        stock = it.stock.toString()
                        description = it.description
                        imageUrl = it.imageUrl
                        selectedCategoryId = it.categoryId
                        selectedCategoryName = categories.find { c -> c.id == it.categoryId }?.name ?: "Danh mục cũ"
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Chỉnh sửa sản phẩm" else "Thêm mới sản phẩm",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading && isEditMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Tên sản phẩm
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên sản phẩm") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Row chứa Giá và Số lượng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        label = { Text("Giá (VNĐ)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { if (it.all { char -> char.isDigit() }) stock = it },
                        label = { Text("Số lượng") },
                        placeholder = { Text("Ví dụ: 10") },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // DROP DOWN CHỌN DANH MỤC
                ExposedDropdownMenuBox(
                    expanded = expandedMenu,
                    onExpandedChange = { expandedMenu = !expandedMenu }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Danh mục sản phẩm") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    selectedCategoryName = cat.name
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }

                // Mô tả
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // URL Hình ảnh
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Link ảnh URL (Firebase Storage)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Xem trước ảnh nếu có URL
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl, contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // NÚT LƯU
                Button(
                    onClick = {
                        if (name.isNotBlank() && price.isNotBlank() && selectedCategoryId.isNotEmpty()) {
                            isLoading = true

                            val stockInt = stock.toIntOrNull() ?: 0

                            val product = ProductItem(
                                id = productId ?: "",
                                name = name,
                                price = price,
                                stock = stockInt,
                                description = description,
                                imageUrl = imageUrl,
                                categoryId = selectedCategoryId
                            )

                            if (isEditMode) {
                                ProductRepository.updateProduct(product) { success ->
                                    isLoading = false
                                    if (success) onBack()
                                }
                            } else {
                                ProductRepository.addProduct(product) { success ->
                                    isLoading = false
                                    if (success) onBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && name.isNotBlank() && price.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (isEditMode) "CẬP NHẬT SẢN PHẨM" else "LƯU VÀO KHO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
