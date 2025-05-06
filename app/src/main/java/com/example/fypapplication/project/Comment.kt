package com.example.fypapplication.project

import java.util.Date

/**
 * Represents a comment on a submission
 */
data class Comment(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "", // STUDENT, SUPERVISOR
    val commentText: String = "",
    val commentDate: Date = Date()
) {
    // Empty constructor required for Firestore
    constructor() : this(
        id = "",
        userId = "",
        userName = "",
        userRole = "",
        commentText = "",
        commentDate = Date()
    )
}