package com.example.weddingapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// File: CustomerScreen.kt
sealed class CustomerScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : CustomerScreen("home", "Trang chủ", Icons.Default.Home)
    object Categories : CustomerScreen("categories", "Danh mục", Icons.Default.GridView)
    object Search : CustomerScreen("search", "Tìm kiếm", Icons.Default.Search)
    object Cart : CustomerScreen("cart", "Giỏ hàng", Icons.Default.ShoppingCart)
    object Orders : CustomerScreen("orders", "Đơn hàng", Icons.Default.Person)
    object Chat : CustomerScreen("chat", "Chat", Icons.Default.Chat)
    object AIs: CustomerScreen("ais", "AI", Icons.Default.SmartToy)
}