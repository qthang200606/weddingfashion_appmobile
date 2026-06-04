package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.repository.CategoryRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CategoriesScreen(onCategoryClick: (String, String) -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())

    // Danh sách các màu gradient sinh động để gán cho từng danh mục
    val categoryGradients = listOf(
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4))), // Hồng đào
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB))), // Xanh dương pastel
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFD4FC79), Color(0xFF96E6A1))), // Xanh lá pastel
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFFECD2), Color(0xFFFCB69F))), // Cam pastel
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4))), // Xanh ngọc
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC)))  // Tím pastel
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFBFBFB))) {
        Text(
            text = "Khám phá danh mục",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
            ),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mục "Tất cả" - Thiết kế đặc biệt với màu tối sang trọng
            item {
                ColorCategoryCard(
                    name = "Tất cả",
                    background = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345))),
                    textColor = Color.White,
                    onClick = {
                        val encodedName = URLEncoder.encode("Tất cả", StandardCharsets.UTF_8.toString())
                        onCategoryClick("all", encodedName)
                    }
                )
            }

            // Các danh mục từ Firebase - Gán màu gradient ngẫu nhiên
            itemsIndexed(categories) { index, category ->
                // Chọn màu dựa trên index để không bị trùng màu lặp lại
                val gradient = categoryGradients[index % categoryGradients.size]

                ColorCategoryCard(
                    name = category.name,
                    background = gradient,
                    onClick = {
                        val encodedName = URLEncoder.encode(category.name, StandardCharsets.UTF_8.toString())
                        onCategoryClick(category.id, encodedName)
                    }
                )
            }
        }
    }
}

@Composable
fun ColorCategoryCard(
    name: String,
    background: androidx.compose.ui.graphics.Brush,
    textColor: Color = Color(0xFF333333),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp) // Chiều cao vừa phải, hiện đại
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(16.dp),
            contentAlignment = Alignment.Center // Tên danh mục ở giữa
        ) {
            Text(
                text = name,
                color = textColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
