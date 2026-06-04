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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

// Màu
val WeddingGold = Color(0xFFD4AF37)
val IvoryWhite = Color(0xFFFFFCF5)

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Eternal Bridal",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                color = WeddingGold
            )
        )

        Text("Đăng nhập để tìm váy cưới mơ ước", color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {

                // Validate
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Login Firebase
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()

                            // ⭐ PHÂN QUYỀN TẠI ĐÂY
                            val role = if (email.trim() == "admin@gmail.com") {
                                "admin"
                            } else {
                                "user"
                            }

                            onLoginSuccess(role)

                        } else {
                            Toast.makeText(
                                context,
                                task.exception?.message ?: "Sai email hoặc mật khẩu",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WeddingGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ĐĂNG NHẬP", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Chưa có tài khoản? Đăng ký ngay", color = WeddingGold)
        }
    }
}