package com.example.fypapplication.project


import java.util.Date

data class Milestone(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,
    val completedDate: Date? = null,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date()
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", null, null, false, Date())
}