package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionManagerScreen(onBack: () -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    val banners by BannerRepository.getBanners().collectAsState(initial = emptyList())

    // --- DIALOG STATES ---
    var showAddCatDialog by remember { mutableStateOf(false) }
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // Loại to ID

    // --- FORM STATES ---
    var newCatName by remember { mutableStateOf("") }
    var editCatName by remember { mutableStateOf("") }
    var bTitle by remember { mutableStateOf("") }
    var bUrl by remember { mutableStateOf("") }
    var bTag by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giao diện & Khuyến mãi", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp)
        ) {
            // --- SECTION 1: BANNERS ---
            item {
                SectionHeader("Banner Quảng cáo") { showAddBannerDialog = true }
                if (banners.isEmpty()) EmptyBox("Chưa có banner nào")
                else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(banners, key = { it.id }) { banner ->
                            BannerItemCard(
                                banner = banner,
                                onToggle = { BannerRepository.updateBannerStatus(banner.id, !banner.isActive) {} },
                                onDelete = { itemToDelete = "Banner" to banner.id }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // --- SECTION 2: CATEGORIES ---
            item { SectionHeader("Danh mục sản phẩm") { showAddCatDialog = true } }

            if (categories.isEmpty()) {
                item { EmptyBox("Chưa có danh mục nào") }
            } else {
                items(categories, key = { it.id }) { category ->
                    CategoryItemCard(
                        category = category,
                        onEdit = {
                            editingCategory = category
                            editCatName = category.name
                        },
                        onDelete = { itemToDelete = "Danh mục" to category.id }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // --- DIALOG LOGIC ---

    // 1. Xác nhận xóa
    itemToDelete?.let { (type, id) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa $type này?") },
            confirmButton = {
                TextButton(onClick = {
                    if (type == "Banner") BannerRepository.deleteBanner(id) {}
                    else CategoryRepository.deleteCategory(id) {}
                    itemToDelete = null
                }) { Text("XÓA", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("HỦY") } }
        )
    }

    // 2. Sửa danh mục
    editingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Sửa danh mục") },
            text = { OutlinedTextField(value = editCatName, onValueChange = { editCatName = it }, label = { Text("Tên mới") }) },
            confirmButton = {
                TextButton(onClick = {
                    CategoryRepository.updateCategory(cat.id, editCatName) { editingCategory = null }
                }) { Text("LƯU") }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("HỦY") } }
        )
    }

    // 3. Thêm Category
    if (showAddCatDialog) {
        AlertDialog(
            onDismissRequest = { showAddCatDialog = false },
            title = { Text("Thêm danh mục") },
            text = { OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Tên danh mục") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) {
                        CategoryRepository.addCategory(newCatName) {
                            newCatName = ""
                            showAddCatDialog = false
                        }
                    }
                }) { Text("THÊM") }
            },
            dismissButton = { TextButton(onClick = { showAddCatDialog = false }) { Text("HỦY") } }
        )
    }

    // 4. Thêm Banner
    if (showAddBannerDialog) {
        AlertDialog(
            onDismissRequest = { showAddBannerDialog = false },
            title = { Text("Thêm Banner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text("Tiêu đề") })
                    OutlinedTextField(value = bUrl, onValueChange = { bUrl = it }, label = { Text("URL hình ảnh") })
                    OutlinedTextField(value = bTag, onValueChange = { bTag = it }, label = { Text("Tag (Giảm 30%...)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (bUrl.isNotBlank()) {
                        val nb = PromoBanner(title = bTitle, imageUrl = bUrl, discountTag = bTag, isActive = true)
                        BannerRepository.addBanner(nb) {
                            bTitle = ""; bUrl = ""; bTag = ""
                            showAddBannerDialog = false
                        }
                    }
                }) { Text("THÊM") }
            },
            dismissButton = { TextButton(onClick = { showAddBannerDialog = false }) { Text("HỦY") } }
        )
    }
}

@Composable
fun SectionHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, null, tint = Color(0xFFD4AF37))
        }
    }
}

@Composable
fun EmptyBox(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.Gray)
    }
}

@Composable
fun BannerItemCard(banner: PromoBanner, onToggle: () -> Unit, onDelete: () -> Unit) {
    // TẠO STATE CỤC BỘ: UI sẽ dùng biến này để hiển thị thay vì đợi Firebase
    var localIsActive by remember { mutableStateOf(banner.isActive) }

    // ĐỒNG BỘ: Nếu dữ liệu từ server thay đổi (ví dụ Admin khác gạt), UI sẽ cập nhật theo
    LaunchedEffect(banner.isActive) {
        localIsActive = banner.isActive
    }

    Card(
        modifier = Modifier.width(300.dp).height(160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Dùng localIsActive để làm mờ ảnh ngay lập tức khi gạt tắt
                    .alpha(if (localIsActive) 1f else 0.4f)
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (banner.discountTag.isNotEmpty()) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                            Text(banner.discountTag, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(banner.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                    // SWITCH SỬ DỤNG LOCAL STATE
                    Switch(
                        checked = localIsActive,
                        onCheckedChange = {
                            localIsActive = it // 1. UI đổi ngay lập tức (mượt mà)
                            onToggle()        // 2. Gọi hàm lưu lên Firebase ngầm
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun CategoryItemCard(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Label, contentDescription = null, tint = Color(0xFFD4AF37))
            Spacer(Modifier.width(12.dp))
            Text(category.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
        }
    }
}
