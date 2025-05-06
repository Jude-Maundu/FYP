package com.example.fypapplication.project

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    var projectTitle: String = "",  // Made mutable with var
    val projectId: String = "",
    val department: String = "",
    val profileImageUrl: String = ""
)