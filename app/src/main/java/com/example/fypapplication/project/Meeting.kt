

package com.example.fypapplication.project

import java.util.Date

data class Meeting(
    val id: String = "",
    val projectId: String = "",
    val studentId: String = "",
    val supervisorId: String = "",
    val title: String = "",
    val description: String = "",
    val meetingDate: Date = Date(),
    val duration: Int = 30, // in minutes
    val location: String = "", // could be physical location or online meeting link
    val status: String = "pending", // pending, confirmed, cancelled, completed
    val notes: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", Date(), 30, "", "pending", "", Date(), Date())
}