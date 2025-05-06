package com.example.fypapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")

    data class UserProfile(
        val id: String = "",
        val name: String = "",
        val email: String = "",
        val role: String = "", // "STUDENT", "SUPERVISOR", "ADMIN"
        val department: String = "",
        val profileImageUrl: String = ""
    )

    /**
     * Get user profile by user ID
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = usersCollection.document(userId).get().await()
                val userProfile = doc.toObject(UserProfile::class.java)
                    ?: throw IllegalStateException("User profile not found")

                Result.success(userProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get current user profile
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                getUserProfile(currentUserId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                usersCollection.document(userProfile.id)
                    .set(userProfile)
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get all supervisors
     */
    suspend fun getAllSupervisors(): Result<List<UserProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = usersCollection
                    .whereEqualTo("role", "SUPERVISOR")
                    .get()
                    .await()

                val supervisors = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                }

                Result.success(supervisors)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get students supervised by a specific supervisor
     */
    suspend fun getStudentsForSupervisor(supervisorId: String): Result<List<UserProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                // Query the projects collection to find projects with this supervisor
                val projectsRef = firestore.collection("projects")
                val projectsSnapshot = projectsRef
                    .whereEqualTo("supervisorId", supervisorId)
                    .get()
                    .await()

                // Extract student IDs from projects
                val studentIds = projectsSnapshot.documents.mapNotNull { doc ->
                    doc.getString("studentId")
                }.distinct()

                // If no students, return empty list
                if (studentIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // Fetch student profiles
                val students = mutableListOf<UserProfile>()

                for (studentId in studentIds) {
                    val userResult = getUserProfile(studentId)
                    if (userResult.isSuccess) {
                        userResult.getOrNull()?.let {
                            students.add(it)
                        }
                    }
                }

                Result.success(students)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}