package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.ProductItem
import com.example.weddingapp.data.repository.ProductRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onProductClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by ProductRepository.searchProducts(searchQuery).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Ô nhập tìm kiếm
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm váy cưới, trang sức...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Danh sách kết quả
        if (searchQuery.isEmpty()) {
            EmptySearchState("Nhập tên sản phẩm để tìm kiếm")
        } else if (searchResults.isEmpty()) {
            EmptySearchState("Không tìm thấy sản phẩm nào khớp với '$searchQuery'")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults, key = { it.id }) { product ->
                    SearchItemCard(product, onProductClick)
                }
            }
        }
    }
}

@Composable
fun SearchItemCard(product: ProductItem, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(product.id) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${formatSearchPrice(product.price)} VNĐ", color = Color(0xFFE91E63), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EmptySearchState(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

fun formatSearchPrice(price: String): String {
    return try {
        val amount = price.toLong()
        java.text.DecimalFormat("#,###").format(amount).replace(",", ".")
    } catch (e: Exception) {
        price
    }
}
