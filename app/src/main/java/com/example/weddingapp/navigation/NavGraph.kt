package com.example.weddingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.weddingapp.ui.screens.admin.*
import com.example.weddingapp.ui.screens.auth.*
import com.example.weddingapp.ui.screens.customer.*
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // --- 1. AUTHENTICATION ---
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { role ->
                    val destination = if (role == "admin") Screen.AdminHub.route else Screen.MainHub.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }

        // --- 2. CUSTOMER FLOW ---
        composable(Screen.MainHub.route) {
            CustomerMainContainer(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                rootNavController = navController
            )
        }

        composable("order_history_screen") {
            OrderHistoryScreen(
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate("order_detail/$orderId")
                }
            )
        }

        composable(
            route = "order_detail/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("checkout_screen") {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderSuccess = {
                    navController.navigate(Screen.MainHub.route) {
                        popUpTo(Screen.MainHub.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "product_list/{catId}/{catName}",
            arguments = listOf(
                navArgument("catId") { type = NavType.StringType },
                navArgument("catName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getString("catId") ?: "all"
            val encodedName = backStackEntry.arguments?.getString("catName") ?: "Sản phẩm"
            val decodedName = try { URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { encodedName }

            ProductListScreen(
                categoryId = catId,
                categoryName = decodedName,
                onBack = { navController.popBackStack() },
                onProductClick = { productId -> navController.navigate("product_detail/$productId") }
            )
        }

        composable(
            route = "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { id ->
                    // Khách hàng chat thì chatId chính là UID của khách
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    navController.navigate("chat_box/$uid/customer?productId=$id")
                }
            )
        }

        composable(
            route = "chat_box/{chatId}/{role}?productId={productId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: "customer"
            val productId = backStackEntry.arguments?.getString("productId")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            ChatBoxScreen(
                chatId = chatId,
                productId = productId,
                currentUserId = currentUserId,
                onBack = { navController.popBackStack() },
                onNavigateToBooking = { navController.navigate("booking_screen") }
            )
        }

        composable("booking_screen") {
            BookingScreen(onBack = { navController.popBackStack() })
        }

        // --- 3. ADMIN FLOW ---
        composable(Screen.AdminHub.route) {
            AdminScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("admin_chat_list") {
            ChatListScreen(
                onNavigateToDetail = { customerId ->
                    navController.navigate("chat_box/$customerId/admin")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppointmentManagement.route) {
            AppointmentScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.InventoryManagement.route) {
            InventoryManagementScreen (onBack = { navController.popBackStack()} )
        }

        composable(Screen.OrderManagement.route) {
            OrderManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.ProductManagement.route) {
            ProductManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAdd = { navController.navigate(Screen.AddEditProduct.createRoute(null)) },
                onNavigateToEdit = { id -> navController.navigate(Screen.AddEditProduct.createRoute(id)) }
            )
        }

        composable(
            route = Screen.AddEditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").let { if (it == "null") null else it }
            AddEditProductScreen(productId = productId, onBack = { navController.popBackStack() })
        }

        composable(Screen.PromotionManager.route) { PromotionManagerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Statistics.route) { StatisticsScreen(onBack = { navController.popBackStack() }) }
    }
}
