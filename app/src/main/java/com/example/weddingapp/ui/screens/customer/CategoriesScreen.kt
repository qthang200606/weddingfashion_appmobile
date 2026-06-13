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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.example.weddingapp.data.repository.CategoryRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CategoriesScreen(onCategoryClick: (String, String) -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())

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
                    imageUrl = "",
                    textColor = Color.White,
                    onClick = {
                        val encodedName = URLEncoder.encode("Tất cả", StandardCharsets.UTF_8.toString())
                        onCategoryClick("all", encodedName)
                    }
                )
            }

            // Các danh mục từ Firebase - Hiển thị ảnh thực tế
            itemsIndexed(categories) { index, category ->
                ColorCategoryCard(
                    name = category.name,
                    imageUrl = category.imageUrl,
                    textColor = Color.White,
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
    imageUrl: String,
    textColor: Color = Color(0xFF333333),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Hiển thị ảnh nếu có
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback gradient cho "Tất cả"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345)))
                        )
                )
            }

            // Overlay gradient để text dễ đọc
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            ),
                            startY = 50f
                        )
                    )
            )

            // Tên danh mục
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
