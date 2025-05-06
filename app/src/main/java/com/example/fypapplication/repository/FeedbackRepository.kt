package com.example.fypapplication.repository

import android.util.Log
import com.example.fypapplication.project.Feedback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FeedbackRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val feedbackCollection = firestore.collection("feedback")

    private val tag = "FeedbackRepository"

    /**
     * Get current user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Get all feedback for the current student
     */
    suspend fun getStudentFeedback(): Result<List<Feedback>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    Log.e(tag, "User not logged in")
                    return@withContext Result.failure(IllegalStateException("User not logged in"))
                }

                Log.d(tag, "Getting feedback for student: $currentUserId")

                // Try a simpler query first to test if basic functionality works
                val snapshot = try {
                    feedbackCollection
                        .whereEqualTo("studentId", currentUserId)
                        .get()
                        .await()
                } catch (e: Exception) {
                    // If simple query fails, log the error and try without orderBy
                    Log.w(tag, "Simple query failed, trying alternative approach", e)
                    feedbackCollection
                        .whereEqualTo("studentId", currentUserId)
                        .get()
                        .await()
                }

                Log.d(tag, "Query returned ${snapshot.documents.size} feedback documents")

                val feedbackItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Feedback::class.java)
                    } catch (e: Exception) {
                        Log.e(tag, "Error converting document ${doc.id}", e)
                        null
                    }
                }

                // Sort the results in memory instead of using Firestore's orderBy
                val sortedFeedbackItems = feedbackItems.sortedByDescending { it.createdAt }

                Log.d(tag, "Successfully mapped ${sortedFeedbackItems.size} feedback items")
                Result.success(sortedFeedbackItems)
            } catch (e: Exception) {
                Log.e(tag, "Error getting student feedback", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get feedback by supervisor
     */
    suspend fun getSupervisorFeedback(supervisorId: String): Result<List<Feedback>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Getting feedback for supervisor: $supervisorId")

                // Use a simpler query to avoid index requirements
                val snapshot = feedbackCollection
                    .whereEqualTo("supervisorId", supervisorId)
                    .get()
                    .await()

                val feedbackItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Feedback::class.java)
                    } catch (e: Exception) {
                        Log.e(tag, "Error converting document ${doc.id}", e)
                        null
                    }
                }

                // Sort the results in memory instead of using Firestore's orderBy
                val sortedFeedbackItems = feedbackItems.sortedByDescending { it.createdAt }

                Result.success(sortedFeedbackItems)
            } catch (e: Exception) {
                Log.e(tag, "Error getting supervisor feedback", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get feedback for a specific project
     */
    suspend fun getProjectFeedback(projectId: String): Result<List<Feedback>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Getting feedback for project: $projectId")

                // Use a simpler query to avoid index requirements
                val snapshot = feedbackCollection
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()

                val feedbackItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Feedback::class.java)
                    } catch (e: Exception) {
                        Log.e(tag, "Error converting document ${doc.id}", e)
                        null
                    }
                }

                // Sort the results in memory instead of using Firestore's orderBy
                val sortedFeedbackItems = feedbackItems.sortedByDescending { it.createdAt }

                Result.success(sortedFeedbackItems)
            } catch (e: Exception) {
                Log.e(tag, "Error getting project feedback", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Create new feedback
     */
    suspend fun createFeedback(feedback: Feedback): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Note: No need to set createdAt as it will be handled by @ServerTimestamp
                // No need to set id as it will be handled by @DocumentId

                // Set isRead to false by default for new feedback
                val feedbackToCreate = feedback.copy(isRead = false)

                val docRef = feedbackCollection.add(feedbackToCreate).await()
                Log.d(tag, "Successfully created feedback with ID: ${docRef.id}")
                Result.success(docRef.id)
            } catch (e: Exception) {
                Log.e(tag, "Error creating feedback", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Mark feedback as read
     */
    suspend fun markFeedbackAsRead(feedbackId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Marking feedback as read: $feedbackId")

                feedbackCollection.document(feedbackId)
                    .update("isRead", true)
                    .await()

                Log.d(tag, "Successfully marked feedback as read: $feedbackId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Error marking feedback as read: $feedbackId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete feedback
     */
    suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Deleting feedback: $feedbackId")

                feedbackCollection.document(feedbackId).delete().await()

                Log.d(tag, "Successfully deleted feedback: $feedbackId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Error deleting feedback: $feedbackId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get unread feedback count for current student
     */
    suspend fun getUnreadFeedbackCount(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    Log.e(tag, "User not logged in")
                    return@withContext Result.failure(IllegalStateException("User not logged in"))
                }

                // Count documents that are unread for this student
                val snapshot = feedbackCollection
                    .whereEqualTo("studentId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                Result.success(snapshot.size())
            } catch (e: Exception) {
                Log.e(tag, "Error getting unread feedback count", e)
                Result.failure(e)
            }
        }
    }
}