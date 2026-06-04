package com.example.weddingapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var weddingDate by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFCF5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Tạo tài khoản mới",
            style = TextStyle(
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
                color = Color(0xFFD4AF37)
            )
        )

        Text("Bắt đầu hành trình tìm váy cưới", color = Color.Gray)

        Spacer(modifier = Modifier.height(30.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Họ và tên") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))


        Button(
            onClick = {
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                } else if (password.length < 6) {
                    Toast.makeText(context, "Mật khẩu phải ≥ 6 ký tự", Toast.LENGTH_SHORT).show()
                } else {

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val userId = auth.currentUser?.uid

                                // 👇 PHÂN QUYỀN
                                val role = if (email == "admin@gmail.com") "admin" else "user"

                                val userMap = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "weddingDate" to weddingDate,
                                    "role" to role
                                )

                                if (userId != null) {
                                    db.collection("users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                                            onNavigateToLogin()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Lỗi lưu Firestore", Toast.LENGTH_SHORT).show()
                                        }
                                }

                            } else {
                                Toast.makeText(
                                    context,
                                    task.exception?.message ?: "Đăng ký thất bại",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ĐĂNG KÝ NGAY", color = Color.White, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập", color = Color(0xFFD4AF37))
        }
    }
}