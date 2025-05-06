package com.example.fypapplication.repository

import android.net.Uri
import android.util.Log
import com.example.fypapplication.project.Comment
import com.example.fypapplication.project.Submission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class SubmissionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val submissionsCollection = firestore.collection("submissions")
    private val projectsCollection = firestore.collection("projects")
    private val storageRef = storage.reference.child("submissions")

    private val TAG = "SubmissionRepository"

    /**
     * Create a new submission with optional file attachments
     */
    suspend fun createSubmission(submission: Submission, fileUris: List<Uri>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating submission for project: ${submission.projectId}")

                // Verify project exists
                val projectExists = verifyProjectExists(submission.projectId)
                if (!projectExists) {
                    Log.e(TAG, "Project does not exist: ${submission.projectId}")
                    throw IllegalArgumentException("Project does not exist")
                }

                // Upload files if any
                val fileUrls = mutableListOf<String>()

                for (fileUri in fileUris) {
                    try {
                        val fileName = "${UUID.randomUUID()}_${fileUri.lastPathSegment}"
                        val fileRef = storageRef.child(submission.projectId).child(fileName)

                        // Upload file
                        Log.d(TAG, "Uploading file: $fileName")
                        val uploadTask = fileRef.putFile(fileUri).await()

                        // Get download URL
                        val downloadUrl = fileRef.downloadUrl.await().toString()
                        fileUrls.add(downloadUrl)
                        Log.d(TAG, "File uploaded successfully: $downloadUrl")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading file", e)
                        // Continue with other files
                    }
                }

                // Create submission document with file URLs
                val submissionId = UUID.randomUUID().toString()
                val submissionWithFiles = submission.copy(
                    id = submissionId,
                    fileUrls = fileUrls,
                    submissionDate = Date()
                )

                Log.d(TAG, "Saving submission to Firestore with ID: $submissionId")

                // Add to Firestore with the generated ID
                submissionsCollection.document(submissionId)
                    .set(submissionWithFiles)
                    .await()

                Log.d(TAG, "Submission created successfully")
                Result.success(submissionId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating submission", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verify that a project exists in Firestore
     */
    private suspend fun verifyProjectExists(projectId: String): Boolean {
        return try {
            val projectDoc = projectsCollection.document(projectId).get().await()
            projectDoc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying project existence", e)
            false
        }
    }

    /**
     * Get all submissions for a specific project
     */
    suspend fun getSubmissionsForProject(projectId: String): Result<List<Submission>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting submissions for project: $projectId")

                if (projectId.isEmpty()) {
                    Log.e(TAG, "Project ID is empty")
                    throw IllegalArgumentException("Project ID cannot be empty")
                }

                // Ensure user is logged in
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    Log.e(TAG, "User not logged in")
                    throw IllegalStateException("User not logged in")
                }

                Log.d(TAG, "Current user ID: $currentUserId")

                // First check if the project exists
                val projectExists = verifyProjectExists(projectId)
                if (!projectExists) {
                    Log.e(TAG, "Project not found: $projectId")
                    throw IllegalArgumentException("Project not found")
                }

                // Get the project to verify access
                val projectDoc = projectsCollection.document(projectId).get().await()
                val studentId = projectDoc.getString("studentId") ?: ""
                val supervisorId = projectDoc.getString("supervisorId") ?: ""

                Log.d(TAG, "Project details - studentId: $studentId, supervisorId: $supervisorId")

                // Verify user has permission to access this project
                if (currentUserId != studentId && currentUserId != supervisorId) {
                    Log.e(TAG, "User $currentUserId does not have permission to access project $projectId")
                    throw SecurityException("You don't have permission to access this project")
                }

                // Query submissions
                Log.d(TAG, "Querying Firestore for submissions where projectId=$projectId")

                // Debug the collection
                val allSubmissions = submissionsCollection.get().await()
                Log.d(TAG, "Total submissions in collection: ${allSubmissions.documents.size}")

                val query = submissionsCollection
                    .whereEqualTo("projectId", projectId)
                    .orderBy("submissionDate", Query.Direction.DESCENDING)

                Log.d(TAG, "Query: ${query.toString()}")

                val snapshot = query.get().await()

                Log.d(TAG, "Query completed. Found ${snapshot.documents.size} submissions")

                // Log document IDs for debugging
                snapshot.documents.forEach { doc ->
                    Log.d(TAG, "Document ID: ${doc.id}")
                }

                val submissions = snapshot.documents.mapNotNull { doc ->
                    try {
                        val submission = doc.toObject(Submission::class.java)
                        if (submission != null) {
                            // Ensure ID is set correctly
                            if (submission.id.isEmpty()) {
                                submission.id = doc.id
                            }
                            Log.d(TAG, "Mapped submission - ID: ${submission.id}, Title: ${submission.title}")
                            submission
                        } else {
                            Log.e(TAG, "Failed to map document to Submission: ${doc.id}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping document ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "Successfully mapped ${submissions.size} submissions")
                Result.success(submissions)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting submissions for project: $projectId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get all submissions for the current student
     */
    suspend fun getStudentSubmissions(): Result<List<Submission>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                Log.d(TAG, "Getting submissions for student: $currentUserId")

                val snapshot = submissionsCollection
                    .whereEqualTo("studentId", currentUserId)
                    .orderBy("submissionDate", Query.Direction.DESCENDING)
                    .get()
                    .await()

                Log.d(TAG, "Found ${snapshot.documents.size} submissions for student")

                val submissions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Submission::class.java)?.also {
                        // Ensure ID is set correctly
                        if (it.id.isEmpty()) {
                            it.id = doc.id
                        }
                    }
                }

                Result.success(submissions)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting student submissions", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get all submissions for a supervisor to review
     */
    suspend fun getSupervisorSubmissions(): Result<List<Submission>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                Log.d(TAG, "Getting submissions for supervisor: $currentUserId")

                val snapshot = submissionsCollection
                    .whereEqualTo("supervisorId", currentUserId)
                    .orderBy("submissionDate", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val submissions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Submission::class.java)?.also {
                        if (it.id.isEmpty()) {
                            it.id = doc.id
                        }
                    }
                }

                Result.success(submissions)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting supervisor submissions", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get pending submissions for a supervisor to review
     */
    suspend fun getPendingSubmissions(): Result<List<Submission>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                val snapshot = submissionsCollection
                    .whereEqualTo("supervisorId", currentUserId)
                    .whereEqualTo("status", "PENDING")
                    .orderBy("submissionDate", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val submissions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Submission::class.java)?.also {
                        if (it.id.isEmpty()) {
                            it.id = doc.id
                        }
                    }
                }

                Result.success(submissions)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting pending submissions", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get a specific submission by its ID
     */
    suspend fun getSubmission(submissionId: String): Result<Submission> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting submission with ID: $submissionId")

                if (submissionId.isEmpty()) {
                    throw IllegalArgumentException("Submission ID cannot be empty")
                }

                val doc = submissionsCollection.document(submissionId).get().await()
                if (!doc.exists()) {
                    throw IllegalStateException("Submission not found with ID: $submissionId")
                }

                val submission = doc.toObject(Submission::class.java)
                    ?: throw IllegalStateException("Failed to parse submission with ID: $submissionId")

                // Ensure ID is set correctly
                if (submission.id.isEmpty()) {
                    submission.id = submissionId
                }

                Result.success(submission)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting submission: $submissionId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update submission status and provide feedback (for supervisors)
     */
    suspend fun updateSubmissionStatus(
        submissionId: String,
        status: String,
        feedback: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating submission $submissionId status to: $status")

                submissionsCollection.document(submissionId)
                    .update(
                        mapOf(
                            "status" to status,
                            "feedback" to feedback,
                            "feedbackDate" to Date()
                        )
                    )
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating submission status", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Grade a submission (for supervisors)
     */
    suspend fun gradeSubmission(submissionId: String, grade: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Grading submission $submissionId with grade: $grade")

                submissionsCollection.document(submissionId)
                    .update("grade", grade)
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error grading submission", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Add a comment to a submission
     */
    suspend fun addComment(submissionId: String, comment: Comment): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Adding comment to submission: $submissionId")

                // Get the current submission
                val submissionDoc = submissionsCollection.document(submissionId).get().await()
                val submission = submissionDoc.toObject(Submission::class.java)
                    ?: throw IllegalStateException("Submission not found with ID: $submissionId")

                // Create a new comment with a unique ID
                val newComment = comment.copy(id = UUID.randomUUID().toString())

                // Add the comment to the submission's comments list
                val updatedComments = submission.comments + newComment

                // Update the submission in Firestore
                submissionsCollection.document(submissionId)
                    .update("comments", updatedComments)
                    .await()

                Log.d(TAG, "Comment added successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a submission (if allowed by policy)
     */
    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting submission: $submissionId")

                val submission = getSubmission(submissionId).getOrThrow()

                // Verify current user is allowed to delete this submission
                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                if (submission.studentId != currentUserId && submission.supervisorId != currentUserId) {
                    throw SecurityException("Not authorized to delete this submission")
                }

                // Delete associated files from storage
                for (fileUrl in submission.fileUrls) {
                    try {
                        val fileRef = storage.getReferenceFromUrl(fileUrl)
                        fileRef.delete().await()
                        Log.d(TAG, "Deleted file: $fileUrl")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting file: $fileUrl", e)
                        // Continue with other files even if one fails
                    }
                }

                // Delete the submission document
                submissionsCollection.document(submissionId).delete().await()
                Log.d(TAG, "Submission deleted successfully")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting submission", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Creates a test submission in the database - for debugging
     */
    suspend fun createTestSubmission(projectId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating test submission for project: $projectId")

                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                // Get supervisor ID from project
                val projectDoc = projectsCollection.document(projectId).get().await()
                if (!projectDoc.exists()) {
                    throw IllegalArgumentException("Project not found")
                }

                val supervisorId = projectDoc.getString("supervisorId") ?: ""
                if (supervisorId.isEmpty()) {
                    throw IllegalArgumentException("Project has no supervisor")
                }

                // Create a test submission
                val submissionId = UUID.randomUUID().toString()
                val submission = Submission(
                    id = submissionId,
                    projectId = projectId,
                    studentId = currentUserId,
                    supervisorId = supervisorId,
                    title = "Test Submission",
                    description = "This is a test submission created for debugging purposes",
                    submissionType = "Test",
                    submissionDate = Date(),
                    status = "PENDING"
                )

                // Save to Firestore
                submissionsCollection.document(submissionId)
                    .set(submission)
                    .await()

                Log.d(TAG, "Test submission created with ID: $submissionId")
                Result.success(submissionId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test submission", e)
                Result.failure(e)
            }
        }
    }
}