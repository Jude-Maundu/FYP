package com.example.fypapplication.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.example.fypapplication.student.StudentDashboardActivity
import com.example.fypapplication.supervisor.SupervisorDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewRegisterLink: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Show toast if redirected from registration
        if (intent.getBooleanExtra("registered", false)) {
            Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink)

        buttonLogin.setOnClickListener {
            loginUser()
        }

        textViewRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        buttonLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    Log.d(TAG, "Login successful for user: $email with UID: $uid")

                    // Query users collection to find the document with matching UID
                    firestore.collection("users")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            if (!userDocs.isEmpty) {
                                // User found in users collection by UID
                                val userDoc = userDocs.documents[0]
                                val role = userDoc.getString("role") ?: "student"

                                Log.d(TAG, "User found by UID. Role: $role")
                                navigateBasedOnRole(role)
                            } else {
                                // No document with matching UID found, try by email
                                Log.d(TAG, "No user found by UID, trying by email")
                                firestore.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener { emailDocs ->
                                        if (!emailDocs.isEmpty) {
                                            val userDoc = emailDocs.documents[0]
                                            val role = userDoc.getString("role") ?: "student"

                                            Log.d(TAG, "User found by email. Role: $role")
                                            navigateBasedOnRole(role)
                                        } else {
                                            // No matching documents found in users collection
                                            // Try the supervisors collection as a fallback
                                            Log.d(TAG, "No user document found, checking supervisors collection")
                                            checkSupervisorsCollection(email, uid)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error querying users by email", e)
                                        Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                        buttonLogin.isEnabled = true
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error querying users collection", e)
                            Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            buttonLogin.isEnabled = true
                        }
                } else {
                    Log.e(TAG, "Login failed", task.exception)
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    buttonLogin.isEnabled = true
                }
            }
    }

    private fun checkSupervisorsCollection(email: String, uid: String) {
        // Check if user exists in supervisors collection
        firestore.collection("supervisors")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { supervisorDocs ->
                if (!supervisorDocs.isEmpty) {
                    Log.d(TAG, "User found in supervisors collection by UID")
                    navigateBasedOnRole("supervisor")
                } else {
                    // Try by email
                    firestore.collection("supervisors")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { emailDocs ->
                            if (!emailDocs.isEmpty) {
                                Log.d(TAG, "User found in supervisors collection by email")
                                navigateBasedOnRole("supervisor")
                            } else {
                                // Default to student if not found anywhere
                                Log.d(TAG, "User not found in any collection, defaulting to student")
                                navigateBasedOnRole("student")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking supervisors by email", e)
                            navigateBasedOnRole("student")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking supervisors collection", e)
                navigateBasedOnRole("student")
            }
    }

    private fun navigateBasedOnRole(role: String) {
        val normalizedRole = role.lowercase()
        Log.d(TAG, "Navigating based on role: $normalizedRole")

        val intent = when (normalizedRole) {
            "supervisor", "lecturer" -> {
                Log.d(TAG, "Routing to SupervisorDashboardActivity")
                Intent(this, SupervisorDashboardActivity::class.java)
            }
            else -> {
                Log.d(TAG, "Routing to StudentDashboardActivity")
                Intent(this, StudentDashboardActivity::class.java)
            }
        }

        startActivity(intent)
        finish()
    }
}