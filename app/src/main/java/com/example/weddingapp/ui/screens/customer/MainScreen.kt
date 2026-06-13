package com.example.weddingapp.ui.screens.customer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.Category
import com.example.weddingapp.data.model.PromoBanner
import com.example.weddingapp.data.repository.BannerRepository
import com.example.weddingapp.data.repository.CategoryRepository
import kotlinx.coroutines.delay
import android.net.Uri
import com.example.weddingapp.R
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCategory: (String) -> Unit
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
        // 1. THANH TOP BAR
        MainTopAppBar(onLogout)

        // 2. VIDEO GIỚI THIỆU PHẦN ĐẦU TRANG
        VideoIntroSection()

        // 5. BANNER KHUYẾN MÃI QUẢNG CÁO
        SectionTitle("Ưu đãi độc quyền")
        if (banners.isNotEmpty()) {
            BannerCarousel(banners)
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray))
        }

        // 4. BỘ SƯU TẬP ĐẶC SẮC
        SectionTitle("Bộ sưu tập nổi bật")
        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    CollectionCard(
                        title = category.name,
                        imageUrl = category.imageUrl
                    ) {
                        onNavigateToCategory(category.id)
                    }
                }
            }
        } else {
            ShimmerLoadingRow()
        }

    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4A4A4A)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(onLogout: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text("L'Amour Wedding", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD4AF37), letterSpacing = 1.sp)
        },
        actions = {
            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFF9E7E38)) }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun VideoIntroSection() {
    val context = LocalContext.current

    val videoUri = Uri.parse(
        "android.resource://${context.packageName}/${R.raw.w}"
    )

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
@Composable
fun CategoryChip(category: Category, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(85.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(Color(0xFFFFF6EE), Color(0xFFF3E5D8)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(28.dp))
        }
        Text(
            text = category.name,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4A4A4A),
            maxLines = 1
        )
    }
}

@Composable
fun CollectionCard(title: String, imageUrl: String, onClick: () -> Unit) {
    val scaleAnimate by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    Card(
        modifier = Modifier
            .width(220.dp)
            .height(290.dp)
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl.ifEmpty { "https://bellabridal.vn/public/upload/files/VID08406_resize(3).jpg" },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scaleAnimate),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 400f
                        )
                    )
            )
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BannerCarousel(banners: List<PromoBanner>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(Unit) {
        while(true) {
            delay(4000)
            if (banners.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .padding(horizontal = 16.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val banner = banners[page]
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        ))
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        if (banner.discountTag.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    banner.discountTag,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(banner.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0))
            )
        }
    }
}


