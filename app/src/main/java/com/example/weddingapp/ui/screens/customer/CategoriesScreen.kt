package com.example.weddingapp.ui.screens.customer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.weddingapp.R // Đảm bảo import đúng R của project để nhận diện thư mục raw
import com.example.weddingapp.data.repository.CategoryRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Composable
fun CategoriesScreen(onCategoryClick: (String, String) -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Cập nhật ExoPlayer đọc file w.mp4 trực tiếp từ thư mục raw local
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Định dạng chuỗi Uri chuẩn cho thư mục tài nguyên raw local
            val rawVideoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.w}")
            setMediaItem(MediaItem.fromUri(rawVideoUri))
            repeatMode = Player.REPEAT_MODE_ALL // Lặp lại vô hạn
            volume = 0f // Tắt tiếng để giữ không gian trải nghiệm sang trọng
            prepare()
            playWhenReady = true
        }
    }

    // Giải phóng bộ nhớ Player khi Screen bị đóng/huỷ
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFCFCFC))) {
        Text(
            text = "Khám phá danh mục",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111111),
                letterSpacing = (-0.5).sp
            ),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // --- VIDEO BANNER ĐẦU TRANG LẤY TỪ VIDEO LOCAL W.MP4 ---
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    background = android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Lớp phủ Gradient mờ tinh xảo nghệ thuật
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )

                        // Nhãn chữ L'Amour đè lên góc dưới video
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "L'Amour Collection 2026",
                                color = Color(0xFFD4AF37),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Đỉnh Cao Kiệt Tác Ngày Chung Đôi",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- DANH MỤC "TẤT CẢ" ---
            item {
                LuxuryCategoryCard(
                    name = "Tất cả trang phục",
                    imageUrl = "",
                    onClick = {
                        val encodedName = URLEncoder.encode("Tất cả", StandardCharsets.UTF_8.toString())
                        onCategoryClick("all", encodedName)
                    }
                )
            }

            // --- DANH MỤC LẤY ĐỘNG TỪ FIREBASE ---
            itemsIndexed(categories) { _, category ->
                LuxuryCategoryCard(
                    name = category.name,
                    imageUrl = category.imageUrl,
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
fun LuxuryCategoryCard(
    name: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    val formattedName = remember(name) {
        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = if (imageUrl.isEmpty()) BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.7f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = formattedName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF141414), Color(0xFF282828))
                            )
                        )
                )
            }

            // Lớp lọc tối mượt bảo vệ độ tương phản cho text hiển thị rõ nét
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.65f)
                            ),
                            startY = 60f
                        )
                    )
            )

            // Tên danh mục đẩy xuống góc trái dưới + Icon mỏng thanh lịch hướng Đông Bắc
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = if (imageUrl.isEmpty()) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}