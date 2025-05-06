package com.example.fypapplication.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utility class to update supervisor IDs in projects when a supervisor is assigned to a student
 * This class works independently and does not interfere with the admin portal UI
 */
class ProjectSupervisorUpdater {
    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "ProjectSupervisorUpdater"

    /**
     * Updates all projects of a student with the assigned supervisor ID
     * @param studentId The ID of the student
     * @param supervisorId The ID of the assigned supervisor
     * @param callback Optional callback for operation result
     */
    fun updateProjectsWithSupervisor(
        studentId: String,
        supervisorId: String,
        callback: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        // Find all projects for this student
        firestore.collection("projects")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d(tag, "No projects found for student $studentId")
                    callback?.invoke(true, "No projects found for this student")
                    return@addOnSuccessListener
                }

                Log.d(tag, "Found ${querySnapshot.size()} projects to update with supervisor $supervisorId")

                // Create a batch to update all projects at once
                val batch = firestore.batch()

                // Add each project document to the batch
                for (document in querySnapshot.documents) {
                    Log.d(tag, "Adding project ${document.id} to batch update")
                    batch.update(document.reference, "supervisorId", supervisorId)
                }

                // Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(tag, "Successfully updated all projects for student $studentId")
                        callback?.invoke(true, "Updated all projects with supervisor ID")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error updating projects: ${e.message}", e)
                        callback?.invoke(false, "Error updating projects: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error querying projects: ${e.message}", e)
                callback?.invoke(false, "Error querying projects: ${e.message}")
            }
    }
}