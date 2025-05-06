package com.example.fypapplication.backend
import com.example.fypapplication.backend.data.User

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)