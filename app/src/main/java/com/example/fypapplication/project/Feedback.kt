package com.example.fypapplication.project

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing feedback from supervisors to students
 */
data class Feedback(
    @DocumentId
    var id: String = "",

    @PropertyName("studentId")
    val studentId: String = "",

    @PropertyName("supervisorId")
    val supervisorId: String = "",

    @PropertyName("supervisorName")
    val supervisorName: String = "",

    @PropertyName("content")
    val content: String = "",

    @PropertyName("projectId")
    val projectId: String = "",

    @PropertyName("isRead")
    var isRead: Boolean = false,

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Date = Date()
)