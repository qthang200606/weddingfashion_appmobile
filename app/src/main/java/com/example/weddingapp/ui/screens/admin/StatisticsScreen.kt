package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

enum class FilterPeriod { TUAN, THANG, NAM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var selectedPeriod by remember { mutableStateOf(FilterPeriod.TUAN) }
    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch dữ liệu thực từ Firestore một lần duy nhất khi mở màn hình
    LaunchedEffect(Unit) {
        db.collection("orders").get().addOnSuccessListener { snapshot ->
            allOrders = snapshot.toObjects(Order::class.java)
            isLoading = false
        }
    }

    // ------------------------------------------------------------------------
    // LOGIC REAL-TIME THỰC TẾ 100%: TÍNH TOÁN THEO BỘ LỌC ĐƯỢC CHỌN
    // ------------------------------------------------------------------------
    val filteredData = remember(allOrders, selectedPeriod) {
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentWeekOfYear = currentCalendar.get(Calendar.WEEK_OF_YEAR)

        // 1. Lọc danh sách đơn hàng nằm trong mốc thời gian được chọn
        val ordersInPeriod = allOrders.filter { order ->
            val orderCalendar = Calendar.getInstance().apply { timeInMillis = order.timestamp }
            val orderYear = orderCalendar.get(Calendar.YEAR)

            when (selectedPeriod) {
                FilterPeriod.TUAN -> {
                    // Lọc các đơn trong cùng Năm và cùng Tuần hiện tại
                    orderYear == currentYear && orderCalendar.get(Calendar.WEEK_OF_YEAR) == currentWeekOfYear
                }
                FilterPeriod.THANG -> {
                    // Lọc toàn bộ đơn trong Năm hiện tại để chia ra 12 tháng
                    orderYear == currentYear
                }
                FilterPeriod.NAM -> {
                    // Lấy tất cả các đơn hàng để gom nhóm theo từng năm độc lập
                    true
                }
            }
        }

        // 2. Tính toán lại toàn bộ số liệu tổng (Chỉ tính doanh thu các đơn đã "Hoàn thành")
        val totalOrders = ordersInPeriod.size
        val completedOrders = ordersInPeriod.count { it.status == "Hoàn thành" }
        val pendingOrders = ordersInPeriod.count { it.status != "Hoàn thành" }
        val totalRevenue = ordersInPeriod.filter { it.status == "Hoàn thành" }.sumOf { it.totalPrice }

        // 3. Phân bổ dữ liệu hiển thị lên các cột biểu đồ (Doanh thu thực tế)
        val chartBars = when (selectedPeriod) {
            FilterPeriod.TUAN -> {
                // Biểu đồ tuần: Trục X hiển thị từ Thứ 2 -> Chủ Nhật (7 cột thực tế)
                val weekDays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                val revenueMap = MutableList(7) { 0f }

                ordersInPeriod.filter { it.status == "Hoàn thành" }.forEach { order ->
                    val cal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    // Chuyển đổi DAY_OF_WEEK của Java (CN = 1, T2 = 2...) sang index mảng (T2 = 0... CN = 6)
                    val index = if (day == Calendar.SUNDAY) 6 else day - 2
                    if (index in 0..6) {
                        revenueMap[index] += order.totalPrice.toFloat()
                    }
                }
                weekDays.zip(revenueMap)
            }
            FilterPeriod.THANG -> {
                // Biểu đồ tháng: Trục X hiển thị từ Tháng 1 -> Tháng 12 (12 cột thực tế)
                val months = (1..12).map { "T$it" }
                val revenueMap = MutableList(12) { 0f }

                ordersInPeriod.filter { it.status == "Hoàn thành" }.forEach { order ->
                    val cal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                    val monthIndex = cal.get(Calendar.MONTH) // Giá trị chạy từ 0 đến 11
                    if (monthIndex in 0..11) {
                        revenueMap[monthIndex] += order.totalPrice.toFloat()
                    }
                }
                months.zip(revenueMap)
            }
            FilterPeriod.NAM -> {
                // Biểu đồ năm: Gom nhóm động theo các năm thực tế có trong Database của bạn
                val revenueMap = mutableMapOf<String, Float>()
                ordersInPeriod.filter { it.status == "Hoàn thành" }.forEach { order ->
                    val cal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                    val yearStr = cal.get(Calendar.YEAR).toString()
                    revenueMap[yearStr] = (revenueMap[yearStr] ?: 0f) + order.totalPrice.toFloat()
                }
                if (revenueMap.isEmpty()) revenueMap[currentYear.toString()] = 0f
                revenueMap.toList().sortedBy { it.first }
            }
        }

        // 4. Tìm kiếm top sản phẩm dịch vụ đặt nhiều nhất trong giai đoạn lọc này
        val productMap = mutableMapOf<String, Int>()
        ordersInPeriod.forEach { order ->
            order.items.forEach { item ->
                // Khớp chính xác thuộc tính item.name từ cấu trúc class CartItem của bạn
                val pName = item.name.ifEmpty { "Dịch vụ ẩn" }
                productMap[pName] = (productMap[pName] ?: 0) + item.quantity
            }
        }
        val topProducts = productMap.toList().sortedByDescending { it.second }.take(4)

        PeriodCalculatedData(totalRevenue, totalOrders, completedOrders, pendingOrders, chartBars, topProducts)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("THỐNG KÊ DOANH THU", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFFDFBF7)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // THANH TABS BỘ LỌC THỜI GIAN TRÊN CÙNG
                item {
                    PeriodFilterSelector(
                        currentPeriod = selectedPeriod,
                        onPeriodChange = { selectedPeriod = it }
                    )
                }

                // CÁC CON SỐ TỔNG (Nhảy giá trị động hoàn toàn khi bấm đổi bộ lọc)
                item {
                    StatCard(
                        title = "Doanh thu thực tế giai đoạn này",
                        value = String.format(Locale.getDefault(), "%,dđ", filteredData.revenue),
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF4CAF50)
                    )
                }

                item {
                    StatCard(
                        title = "Số lượng đơn đặt hàng giai đoạn này",
                        value = filteredData.ordersCount.toString(),
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF2196F3)
                    )
                }

                item {
                    ChartCard(title = "Cơ cấu trạng thái đơn hàng") {
                        OrderPieChart(completed = filteredData.completedCount, pending = filteredData.pendingCount)
                    }
                }

                // BIỂU ĐỒ CỘT REAL-TIME (Tự động co giãn T2-CN hoặc T1-T12, ghim số liệu rút gọn trên đầu cột)
                item {
                    ChartCard(
                        title = when(selectedPeriod) {
                            FilterPeriod.TUAN -> "Doanh thu các ngày trong tuần này (đ)"
                            FilterPeriod.THANG -> "Doanh thu 12 tháng năm nay (đ)"
                            FilterPeriod.NAM -> "So sánh doanh thu qua các năm (đ)"
                        }
                    ) {
                        DynamicRevenueBarChart(dataPoints = filteredData.chartPoints)
                    }
                }

                item {
                    Text("Dịch vụ yêu thích giai đoạn này", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (filteredData.topProducts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Text("Không có dữ liệu bán hàng trong giai đoạn này", modifier = Modifier.padding(16.dp), color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredData.topProducts) { product ->
                        ProductRankItem(name = product.first, quantity = product.second)
                    }
                }
            }
        }
    }
}

// Data class đóng gói kết quả tính toán động thời gian thực
data class PeriodCalculatedData(
    val revenue: Long,
    val ordersCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val chartPoints: List<Pair<String, Float>>,
    val topProducts: List<Pair<String, Int>>
)

// --- THANH TABS CHỌN ĐIỀU KIỆN ---
@Composable
fun PeriodFilterSelector(currentPeriod: FilterPeriod, onPeriodChange: (FilterPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFEBE4), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        FilterPeriod.values().forEach { period ->
            val label = when(period) {
                FilterPeriod.TUAN -> "Tuần này"
                FilterPeriod.THANG -> "Tháng (Năm nay)"
                FilterPeriod.NAM -> "Theo Năm"
            }
            val isSelected = currentPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) Color(0xFFD4AF37) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onPeriodChange(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// --- BIỂU ĐỒ CỘT VẼ REAL-TIME HIỂN THỊ SỐ TRÊN ĐỈNH ---
@Composable
fun DynamicRevenueBarChart(dataPoints: List<Pair<String, Float>>) {
    if (dataPoints.isEmpty()) return
    val maxRevenue = dataPoints.maxOf { it.second }.let { if (it == 0f) 1f else it }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 28.dp, bottom = 20.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barCount = dataPoints.size

        // Chia khoảng cách cột linh hoạt dựa theo số lượng phần tử của bộ lọc (7 cột hoặc 12 cột)
        val spacing = canvasWidth / (barCount * 2f + 1f)
        val barWidth = spacing

        dataPoints.forEachIndexed { index, point ->
            val label = point.first
            val revenue = point.second

            val barHeight = (revenue / maxRevenue) * (canvasHeight - 40f)
            val xOffset = spacing + (index * (barWidth + spacing))
            val yOffset = canvasHeight - barHeight

            // Vẽ thanh hình chữ nhật biểu thị doanh thu (Màu Vàng Gold đẳng cấp)
            drawRect(
                color = Color(0xFFD4AF37),
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight)
            )

            // Ghi văn bản số liệu trực tiếp lên Canvas đồ thị
            drawContext.canvas.nativeCanvas.apply {
                // 1. Vẽ văn bản số tiền viết tắt trên đỉnh đầu mỗi cột
                val valuePaint = android.graphics.Paint().apply {
                    color = Color(0xFF2D2D2D).toArgb()
                    textSize = if (barCount > 7) 20f else 25f // Hạ font chữ xuống một chút nếu đồ thị là 12 cột của Tháng để tránh đè chữ
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Thu gọn định dạng số tiền (Ví dụ: 15.5M, 350K hoặc ẩn nếu không có doanh thu)
                val formattedText = when {
                    revenue >= 1_000_000f -> String.format(Locale.US, "%.1fM", revenue / 1_000_000f)
                    revenue >= 1_000f -> String.format(Locale.US, "%.0fK", revenue / 1_000f)
                    revenue == 0f -> "" // Nếu mốc thời gian đó bằng 0đ thì ẩn text đi cho thoáng mắt đồ thị
                    else -> String.format(Locale.US, "%.0f", revenue)
                }

                drawText(formattedText, xOffset + (barWidth / 2), yOffset - 10f, valuePaint)

                // 2. Vẽ nhãn mốc thời gian dưới chân cột (Ví dụ: T2-CN hoặc T1-T12)
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = if (barCount > 7) 24f else 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(label, xOffset + (barWidth / 2), canvasHeight + 30f, labelPaint)
            }
        }
    }
}

// --- CÁC UI COMPONENT KHÁC ---
@Composable
fun OrderPieChart(completed: Int, pending: Int) {
    val total = (completed + pending).toFloat()
    val completedAngle = if (total > 0) (completed / total) * 360f else 0f
    val pendingAngle = if (total > 0) (pending / total) * 360f else 0f
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        Canvas(modifier = Modifier.size(100.dp)) {
            if (total == 0f) drawCircle(color = Color.LightGray, radius = size.minDimension / 2)
            else {
                drawArc(color = Color(0xFF4CAF50), startAngle = -90f, sweepAngle = completedAngle, useCenter = true)
                drawArc(color = Color(0xFFFF9800), startAngle = -90f + completedAngle, sweepAngle = pendingAngle, useCenter = true)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IndicatorLabel(color = Color(0xFF4CAF50), label = "Hoàn thành ($completed)")
            IndicatorLabel(color = Color(0xFFFF9800), label = "Đang xử lý ($pending)")
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ProductRankItem(name: String, quantity: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFFFFF6EE), CircleShape), contentAlignment = Alignment.Center) { Text("💍", fontSize = 16.sp) }
            Spacer(Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            Text("Đã ký: $quantity", fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37), fontSize = 13.sp)
        }
    }
}

@Composable
fun IndicatorLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2D2D2D))
            }
        }
    }
}