package com.example.weddingapp.navigation

sealed class Screen(val route: String) {
    // --- Auth Screens ---
    object Login : Screen("login")
    object Register : Screen("register")

    // --- Hub Screens ---
    object MainHub : Screen("main_hub")
    object AdminHub : Screen("admin_hub")

    // --- Admin Management ---
    object ProductManagement : Screen("product_management")
    object CategoryManagement : Screen("category_management")
    object PromotionManager : Screen("promotion_manager")

    // Trang Add/Edit dùng chung một route có tham số productId
    object AddEditProduct : Screen("add_edit_product/{productId}") {
        fun createRoute(productId: String?) = "add_edit_product/${productId ?: "null"}"
    }

    // --- Other Admin Screens ---
    object OrderManagement : Screen("order_management")
    object AppointmentManagement : Screen("appointment_management")
    object Statistics : Screen("statistics")
    object InventoryManagement : Screen("inventory_management")


    // --- Customer Detail ---
    object Detail : Screen("detail/{dressName}") {
        fun createRoute(name: String) = "detail/$name"
    }
    object Checkout : Screen("checkout_screen")
}