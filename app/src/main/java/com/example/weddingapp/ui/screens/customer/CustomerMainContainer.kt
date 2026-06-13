package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.weddingapp.navigation.CustomerScreen
import com.example.weddingapp.data.repository.CartRepository
import com.google.firebase.auth.FirebaseAuth
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CustomerMainContainer(
    onLogout: () -> Unit,
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val cartItems by CartRepository.getCartItems().collectAsState(initial = emptyList())
    val totalItems = cartItems.sumOf { it.quantity }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Scaffold(
        bottomBar = { CustomerBottomBar(navController, totalItems) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CustomerScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CustomerScreen.Home.route) {
                MainScreen(
                    onLogout = onLogout,
                    onNavigateToCategory = { catId ->
                        rootNavController.navigate("product_list/$catId/Sản phẩm")
                    }
                )
            }
            composable(CustomerScreen.Categories.route) {
                CategoriesScreen(onCategoryClick = { id, name ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    rootNavController.navigate("product_list/$id/$encodedName")
                })
            }
            composable(CustomerScreen.Search.route) {
                SearchScreen(
                    onProductClick = { productId ->
                        rootNavController.navigate("product_detail/$productId")
                    }
                )
            }

            composable(CustomerScreen.Cart.route) {
                CartScreen(
                    onBack = {
                        navController.navigate(CustomerScreen.Home.route)
                    },
                    onCheckout = {
                        rootNavController.navigate("checkout_screen")
                    }
                )
            }

            composable(CustomerScreen.Orders.route) {
                OrderHistoryScreen(
                    onBack = {
                        navController.navigate(CustomerScreen.Home.route)
                    },
                    onOrderClick = { orderId: String ->
                        rootNavController.navigate("order_detail/$orderId")
                    }
                )
            }
            composable(CustomerScreen.Chat.route){
                ChatBoxScreen(
                    currentUserId = currentUserId,
                    chatId = currentUserId,
                    onBack = {
                        navController.navigate(CustomerScreen.Home.route)
                    },
                    onNavigateToBooking = {
                        rootNavController.navigate("booking_screen")
                    }
                )
            }
            composable(CustomerScreen.AIs.route){
                WeddingAIScreen(
                    onBack = {
                        navController.navigate(CustomerScreen.Home.route)
                    }
                )
            }
        }
    }
}

@Composable
fun CustomerBottomBar(navController: NavHostController, totalItems: Int) {
    val items = listOf(
        CustomerScreen.Home,
        CustomerScreen.Categories,
        CustomerScreen.Search,
        CustomerScreen.Cart,
        CustomerScreen.Orders,
        CustomerScreen.Chat,
        CustomerScreen.AIs
    )
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFFD4AF37)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    if (screen is CustomerScreen.Cart && totalItems > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(totalItems.toString()) }
                            }
                        ) {
                            Icon(screen.icon, contentDescription = screen.title)
                        }
                    } else {
                        Icon(screen.icon, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title, fontSize = 10.sp) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFD4AF37),
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color(0xFFD4AF37),
                    indicatorColor = Color(0xFFFDFBF7)
                )
            )
        }
    }
}
