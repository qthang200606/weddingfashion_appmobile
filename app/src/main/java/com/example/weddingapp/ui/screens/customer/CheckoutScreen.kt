package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.Order
import com.example.weddingapp.data.repository.CartRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderSuccess: () -> Unit
) {

    val cartItems by CartRepository
        .getCartItems()
        .collectAsState(initial = emptyList())

    val totalPrice = cartItems.sumOf {
        (it.price.replace(".", "").toLongOrNull() ?: 0L) * it.quantity
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }

    var qrUrl by remember { mutableStateOf("") }

    // PAYMENT METHOD
    var paymentMethod by remember {
        mutableStateOf("COD")
    }

    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thanh Toán",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },

        bottomBar = {

            CheckoutBottomBar(
                totalPrice = totalPrice,

                enabled =
                    name.isNotBlank() &&
                            phone.isNotBlank() &&
                            address.isNotBlank() &&
                            !isProcessing,

                isProcessing = isProcessing,

                paymentMethod = paymentMethod,

                onConfirm = {

                    isProcessing = true

                    val orderId =
                        "ORDER_${System.currentTimeMillis()}"

                    val order = Order(
                        id = orderId,

                        userId = FirebaseAuth
                            .getInstance()
                            .currentUser?.uid ?: "",

                        customerName = name,
                        phoneNumber = phone,
                        address = address,
                        items = cartItems,
                        totalPrice = totalPrice,

                        paymentMethod = paymentMethod,

                        paymentStatus =
                            if (paymentMethod == "COD")
                                "UNPAID"
                            else
                                "PENDING"
                    )

                    firestore.collection("orders")
                        .document(orderId)
                        .set(order)

                        .addOnSuccessListener {

                            // CASH ON DELIVERY
                            if (paymentMethod == "COD") {

                                CartRepository.clearCart()

                                onOrderSuccess()
                            }

                            // QR PAYMENT
                            else {

                                qrUrl =
                                    "https://qr.sepay.vn/img?" +
                                            "bank=VietinBank" +
                                            "&acc=106865935777" +
                                            "&template=compact" +
                                            "&amount=$totalPrice" +
                                            "&des=SEVQR%20$orderId"

                                firestore.collection("orders")
                                    .document(orderId)
                                    .addSnapshotListener { snapshot, _ ->

                                        val status =
                                            snapshot?.getString(
                                                "paymentStatus"
                                            )

                                        if (status == "PAID") {

                                            CartRepository.clearCart()

                                            onOrderSuccess()
                                        }
                                    }
                            }
                        }

                        .addOnFailureListener {
                            isProcessing = false
                        }
                }
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // SHIPPING INFO

            SectionCard(title = "Thông tin giao hàng") {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Họ và tên")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = {
                        Text("Số điện thoại")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = {
                        Text("Địa chỉ nhận hàng")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // PAYMENT METHOD

            SectionCard(title = "Phương thức thanh toán") {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = paymentMethod == "COD",
                            onClick = {
                                paymentMethod = "COD"
                            }
                        ),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = paymentMethod == "COD",
                        onClick = {
                            paymentMethod = "COD"
                        }
                    )

                    Text("Nhận hàng thanh toán")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = paymentMethod == "QR",
                            onClick = {
                                paymentMethod = "QR"
                            }
                        ),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = paymentMethod == "QR",
                        onClick = {
                            paymentMethod = "QR"
                        }
                    )

                    Text("Chuyển khoản QR")
                }
            }

            // ORDER SUMMARY

            SectionCard(title = "Tóm tắt đơn hàng") {

                cartItems.forEach { item ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            "${item.name} x${item.quantity}",
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            "${item.price}đ",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        "Phí vận chuyển",
                        color = Color.Gray
                    )

                    Text(
                        "Miễn phí",
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // QR PAYMENT

            if (qrUrl.isNotEmpty() &&
                paymentMethod == "QR"
            ) {

                SectionCard(
                    title = "Quét mã QR để thanh toán"
                ) {

                    Text(
                        text =
                            "Vui lòng chuyển khoản đúng nội dung để hệ thống tự xác nhận.",

                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {

                        AsyncImage(
                            model = qrUrl,
                            contentDescription = null,
                            modifier = Modifier.size(260.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Đang chờ thanh toán...",
                        color = Color(0xFFE91E63),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun CheckoutBottomBar(
    totalPrice: Long,
    enabled: Boolean,
    isProcessing: Boolean,
    paymentMethod: String,
    onConfirm: () -> Unit
) {

    Surface(
        shadowElevation = 10.dp,
        color = Color.White
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    "Tổng thanh toán",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    String.format(
                        Locale.getDefault(),
                        "%,dđ",
                        totalPrice
                    ),

                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE91E63)
                )
            }

            Button(
                onClick = onConfirm,

                enabled = enabled,

                shape = RoundedCornerShape(10.dp),

                modifier = Modifier
                    .height(48.dp)
                    .width(180.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63)
                )
            ) {

                if (isProcessing) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        if (paymentMethod == "QR")
                            "THANH TOÁN QR"
                        else
                            "ĐẶT HÀNG",

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}