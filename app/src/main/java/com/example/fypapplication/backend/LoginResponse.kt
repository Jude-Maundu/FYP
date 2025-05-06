package com.example.fypapplication.backend

data class LoginResponse(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val token: String
)