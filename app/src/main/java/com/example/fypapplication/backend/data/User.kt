package com.example.fypapplication.backend.data

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val token: String? = null
)