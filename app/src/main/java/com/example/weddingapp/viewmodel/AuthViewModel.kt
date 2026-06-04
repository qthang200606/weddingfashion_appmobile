package com.example.weddingapp.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.mutableStateOf

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    // Trạng thái để báo cho UI biết kết quả
    var loginStatus = mutableStateOf<String?>(null)

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            loginStatus.value = "Vui lòng nhập đầy đủ thông tin"
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginStatus.value = "Thành công"
                    onSuccess()
                } else {
                    loginStatus.value = "Lỗi: ${task.exception?.message}"
                }
            }
    }

    fun register(email: String, pass: String, onSuccess: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginStatus.value = "Đăng ký thành công"
                    onSuccess()
                } else {
                    loginStatus.value = "Lỗi đăng ký: ${task.exception?.message}"
                }
            }
    }
}