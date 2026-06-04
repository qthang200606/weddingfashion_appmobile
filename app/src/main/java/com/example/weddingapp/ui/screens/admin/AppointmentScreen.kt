package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Appointment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Lắng nghe danh sách lịch hẹn real-time
    DisposableEffect(Unit) {
        val registration = db.collection("appointments")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    appointments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QUẢN LÝ LỊCH HẸN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") 
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appointments) { item ->
                    AppointmentAdminCard(item)
                }
            }
        }
    }
}

@Composable
fun AppointmentAdminCard(appointment: Appointment) {
    val db = FirebaseFirestore.getInstance()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(appointment.customerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(appointment.phoneNumber, color = Color.Gray, fontSize = 14.sp)
                }
                StatusBadge(appointment.status)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(Icons.Default.Event, appointment.date)
                InfoItem(Icons.Default.AccessTime, appointment.timeSlot)
                InfoItem(Icons.Default.Checkroom, appointment.serviceType)
            }

            if (appointment.status == "Chờ xác nhận") {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // NÚT TỪ CHỐI
                    OutlinedButton(
                        onClick = {
                            updateStatusAndNotify(
                                db = db,
                                appointment = appointment,
                                newStatus = "Từ chối",
                                messageText = "❌ Rất tiếc, shop không thể tiếp nhận lịch hẹn của bạn vào lúc ${appointment.timeSlot} ngày ${appointment.date} do lịch đã kín. Vui lòng chọn thời gian khác nhé!"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) { Text("TỪ CHỐI") }

                    // NÚT XÁC NHẬN
                    Button(
                        onClick = {
                            updateStatusAndNotify(
                                db = db,
                                appointment = appointment,
                                newStatus = "Đã xác nhận",
                                messageText = "✅ THÔNG BÁO: Lịch hẹn của bạn (${appointment.serviceType}) vào lúc ${appointment.timeSlot} ngày ${appointment.date} ĐÃ ĐƯỢC XÁC NHẬN. Hẹn gặp bạn tại shop!"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("XÁC NHẬN") }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Đã xác nhận" -> Color(0xFF4CAF50)
        "Từ chối" -> Color.Red
        else -> Color(0xFFD4AF37)
    }
    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun InfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun updateStatusAndNotify(
    db: FirebaseFirestore,
    appointment: Appointment,
    newStatus: String,
    messageText: String
) {
    // 1. Cập nhật trạng thái trong bảng appointments
    db.collection("appointments").document(appointment.id)
        .update("status", newStatus)

    // 2. Gửi tin nhắn tự động vào khung chat của khách (userId chính là chatId)
    val msgData = hashMapOf(
        "text" to messageText,
        "senderId" to "admin", // Người gửi là admin
        "timestamp" to FieldValue.serverTimestamp(),
        "type" to "TEXT"
    )

    db.collection("chats").document(appointment.userId)
        .collection("messages").add(msgData)

    // 3. Cập nhật lastMessage để khách và admin thấy thông báo mới nhất ở danh sách chat
    db.collection("chats").document(appointment.userId).set(
        hashMapOf(
            "lastMessage" to (if (newStatus == "Đã xác nhận") "✅ Đã xác nhận lịch hẹn" else "❌ Đã từ chối lịch hẹn"),
            "lastTime" to FieldValue.serverTimestamp()
        ), SetOptions.merge()
    )
}
