package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weddingapp.data.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val timeSlots = listOf("08:00", "09:30", "11:00", "13:30", "15:00", "16:30")

    // State cho việc chọn ngày
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Ngày mặc định là ngày hiện tại
    var selectedDateText by remember { mutableStateOf(formatter.format(Date())) }
    var selectedSlot by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Thử đồ") }

    // Dialog chọn ngày
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateText = formatter.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("HỦY") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ĐẶT LỊCH HẸN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text("1. Chọn dịch vụ", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Thử đồ", "Tư vấn cưới").forEach { type ->
                    FilterChip(
                        selected = serviceType == type,
                        onClick = { serviceType = type },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // PHẦN CHỌN NGÀY MỚI
            Text("2. Chọn ngày", fontWeight = FontWeight.Bold)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showDatePicker = true },
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(text = selectedDateText, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("3. Chọn khung giờ", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(timeSlots) { slot ->
                    val isSelected = selectedSlot == slot
                    Card(
                        onClick = { selectedSlot = slot },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFD4AF37) else Color.White
                        ),
                        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null
                    ) {
                        Box(Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = slot,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val user = auth.currentUser
                    val customerId = user?.uid ?: ""

                    val newAppointment = Appointment(
                        userId = customerId,
                        customerName = user?.displayName ?: "Khách hàng",
                        date = selectedDateText,
                        timeSlot = selectedSlot,
                        serviceType = serviceType
                    )

                    db.collection("appointments").add(newAppointment)
                        .addOnSuccessListener {
                            val autoMessage = "📅 YÊU CẦU ĐẶT LỊCH:\n" +
                                    "Dịch vụ: $serviceType\n" +
                                    "Thời gian: $selectedSlot - $selectedDateText\n" +
                                    "Trạng thái: Đang chờ shop xác nhận..."

                            val msgData = hashMapOf(
                                "text" to autoMessage,
                                "senderId" to customerId,
                                "timestamp" to FieldValue.serverTimestamp(),
                                "type" to "BOOKING_CONFIRM"
                            )

                            db.collection("chats").document(customerId).collection("messages").add(msgData)

                            db.collection("chats").document(customerId).set(
                                hashMapOf(
                                    "lastMessage" to "📅 Khách đặt lịch mới: $serviceType",
                                    "lastTime" to FieldValue.serverTimestamp(),
                                    "userId" to customerId
                                ),
                                SetOptions.merge()
                            )

                            onBack()
                        }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = selectedSlot.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
            ) {
                Text("XÁC NHẬN ĐẶT LỊCH")
            }
        }
    }
}