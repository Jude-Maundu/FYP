package com.example.fypapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.login.LoginActivity
import com.example.fypapplication.student.StudentDashboardActivity
import com.example.fypapplication.supervisor.SupervisorDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, check role in Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role") ?: "student"
                        navigateBasedOnRole(role)
                    } else {
                        // No user data found, treat as student by default
                        navigateBasedOnRole("student")
                    }
                }
                .addOnFailureListener {
                    // If we can't get user data, navigate to login
                    navigateToLogin()
                }
        } else {
            // User is not signed in, navigate to login
            navigateToLogin()
        }
    }

    private fun navigateBasedOnRole(role: String) {
        val intent = when (role.lowercase()) {
            "student" -> Intent(this, StudentDashboardActivity::class.java)
            "supervisor", "lecturer" -> Intent(this, SupervisorDashboardActivity::class.java)
            else -> Intent(this, StudentDashboardActivity::class.java) // Default to student
        }

        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}