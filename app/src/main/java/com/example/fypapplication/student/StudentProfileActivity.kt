package com.example.fypapplication.student

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextStudentId: EditText
    private lateinit var editTextDepartment: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var buttonSave: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.profile)

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextStudentId = findViewById(R.id.editTextStudentId)
        editTextDepartment = findViewById(R.id.editTextDepartment)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        buttonSave = findViewById(R.id.buttonSave)

        // Load user profile data
        loadUserProfile()

        // Set up save button
        buttonSave.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Set email and disable editing
        editTextEmail.setText(currentUser.email)
        editTextEmail.isEnabled = false

        // Get additional user data from Firestore
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    editTextName.setText(document.getString("displayName") ?: "")
                    editTextStudentId.setText(document.getString("studentId") ?: "")
                    editTextDepartment.setText(document.getString("department") ?: "")
                    editTextPhone.setText(document.getString("phone") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Validate input fields
        if (validateInputs()) {
            val userMap = hashMapOf(
                "displayName" to editTextName.text.toString().trim(),
                "studentId" to editTextStudentId.text.toString().trim(),
                "department" to editTextDepartment.text.toString().trim(),
                "phone" to editTextPhone.text.toString().trim()
            )

            // Update the user document in Firestore
            firestore.collection("users").document(currentUser.uid)
                .update(userMap as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.profile_updated_successfully, Toast.LENGTH_SHORT).show()
                    finish() // Close the activity after successful update
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate Name
        if (editTextName.text.toString().trim().isEmpty()) {
            editTextName.error = getString(R.string.error_name_required)
            isValid = false
        }

        // Validate Student ID
        if (editTextStudentId.text.toString().trim().isEmpty()) {
            editTextStudentId.error = getString(R.string.error_student_id_required)
            isValid = false
        }

        // Validate Department
        if (editTextDepartment.text.toString().trim().isEmpty()) {
            editTextDepartment.error = getString(R.string.error_department_required)
            isValid = false
        }

        // Validate Phone (optional, but format check if not empty)
        val phoneNumber = editTextPhone.text.toString().trim()
        if (phoneNumber.isNotEmpty() && !android.util.Patterns.PHONE.matcher(phoneNumber).matches()) {
            editTextPhone.error = getString(R.string.error_invalid_phone)
            isValid = false
        }

        return isValid
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}