package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.Category
import com.example.weddingapp.data.model.PromoBanner
import com.example.weddingapp.data.repository.BannerRepository
import com.example.weddingapp.data.repository.CategoryRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val banners by BannerRepository.getBanners().collectAsState(initial = emptyList())
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBF7))
            .verticalScroll(scrollState)
    ) {
        MainTopAppBar(onLogout)

        if (banners.isNotEmpty()) {
            BannerCarousel(banners)
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.LightGray))
        }

        SectionTitle("Danh mục dịch vụ")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryChip(category) { onNavigateToCategory(category.id) }
            }
        }

        SectionTitle("Bộ sưu tập đặc sắc")
        // Giả lập dữ liệu collections vì chưa có repository
        val weddingCollections = listOf(
            WeddingCollection("Váy cưới Luxury", "https://bellabridal.vn/public/upload/files/VID08406_resize(3).jpg"),
            WeddingCollection("Vest Hàn Quốc", "https://bellabridal.vn/public/upload/files/NH_06346_resize(1).jpg")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(weddingCollections) { item ->
                CollectionCard(item) { onNavigateToDetail(item.title) }
            }
        }

        SectionTitle("Cẩm nang ngày cưới")
        NewsSection()

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MainTopAppBar(onLogout: () -> Unit) {
    @OptIn(ExperimentalMaterial3Api::class)
    CenterAlignedTopAppBar(
        title = {
            Text("L'Amour Wedding", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD4AF37))
        },
        actions = {
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null) }
            IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(16.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D2D2D)
    )
}

@Composable
fun BannerCarousel(banners: List<PromoBanner>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(Unit) {
        while(true) {
            delay(3000)
            if (banners.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val banner = banners[page]
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    ))
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
                ) {
                    if (banner.discountTag.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFD4AF37),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                banner.discountTag,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(banner.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: Category, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3E5D8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = Color(0xFFD4AF37))
        }
        Text(
            text = category.name,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CollectionCard(item: WeddingCollection, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.height(250.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Text(
                item.title,
                modifier = Modifier.padding(12.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NewsSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Box(Modifier.size(80.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Bí quyết chọn váy cưới cho cô dâu dáng tròn", fontWeight = FontWeight.Bold, maxLines = 2)
                        Text("Bởi: L'Amour Admin • 2 giờ trước", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class WeddingCollection(val title: String, val imageUrl: String)
