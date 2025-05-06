package com.example.fypapplication.project

import java.util.Date

// This assumes you already have a Project class. This update shows the needed modifications
// to support the supervisor project approval workflow.
data class Project(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val studentId: String = "",
    var studentName: String = "", // Used for display in supervisor view
    val supervisorId: String = "",
    var status: String = "proposed", // proposed, approved, rejected, in_progress, completed
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val objectives: List<String> = listOf()
)