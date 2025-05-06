package com.example.fypapplication.project

import java.util.Date

/**
 * Represents a student's project submission
 */
data class Submission(
    var id: String = "",
    val projectId: String = "",
    val studentId: String = "",
    val supervisorId: String = "",
    val title: String = "",
    val description: String = "",
    val submissionType: String = "",
    val submissionDate: Date = Date(),
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, REVIEWED
    val feedback: String = "",
    val feedbackDate: Date? = null,
    val grade: String = "",
    val fileUrls: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
) {
    // Empty constructor required for Firestore
    constructor() : this(
        id = "",
        projectId = "",
        studentId = "",
        supervisorId = "",
        title = "",
        description = "",
        submissionType = "",
        submissionDate = Date(),
        status = "PENDING",
        feedback = "",
        feedbackDate = null,
        grade = "",
        fileUrls = emptyList(),
        comments = emptyList()
    )
}