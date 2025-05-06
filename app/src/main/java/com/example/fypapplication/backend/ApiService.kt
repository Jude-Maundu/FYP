package com.example.fypapplication.backend

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login/")
    suspend fun loginUser(@Body request: LoginRequest): LoginResponse

    @POST("register/")
    suspend fun registerUser(@Body request: RegisterRequest): RegisterResponse
}