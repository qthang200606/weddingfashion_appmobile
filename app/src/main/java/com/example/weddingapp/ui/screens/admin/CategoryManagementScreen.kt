package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Category
import com.example.weddingapp.data.repository.CategoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(onBack: () -> Unit) {
    val categories by CategoryRepository.getCategories().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh mục đồ cưới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD4AF37)
            ) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có danh mục nào. Hãy thêm mới!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryItem(category)
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Thêm loại đồ mới") },
                text = {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Tên danh mục") },
                        placeholder = { Text("Ví dụ: Áo dài, Phụ kiện...") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCatName.isNotBlank()) {
                            CategoryRepository.addCategory(newCatName) {
                                newCatName = ""
                                showAddDialog = false
                            }
                        }
                    }) { Text("THÊM", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("HỦY") }
                }
            )
        }
    }
}

@Composable
fun CategoryItem(category: Category) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Label, contentDescription = null, tint = Color(0xFFD4AF37))
            Spacer(Modifier.width(16.dp))
            Text(category.name, Modifier.weight(1f), fontSize = 16.sp)
            IconButton(onClick = { CategoryRepository.deleteCategory(category.id) {} }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}
