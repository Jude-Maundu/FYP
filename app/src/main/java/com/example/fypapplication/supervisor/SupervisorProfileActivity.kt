package com.example.fypapplication.supervisor

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SupervisorProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextSupervisorId: EditText
    private lateinit var editTextDepartment: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var buttonSave: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_profile)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.profile)

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextSupervisorId = findViewById(R.id.editTextSupervisorId)
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

        // Disable email editing since it's tied to authentication
        editTextEmail.setText(currentUser.email)
        editTextEmail.isEnabled = false

        // Get additional user data from Firestore
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    editTextName.setText(document.getString("displayName"))
                    editTextSupervisorId.setText(document.getString("supervisorId"))
                    editTextDepartment.setText(document.getString("department"))
                    editTextPhone.setText(document.getString("phone"))
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

        val userMap = hashMapOf(
            "displayName" to editTextName.text.toString(),
            "supervisorId" to editTextSupervisorId.text.toString(),
            "department" to editTextDepartment.text.toString(),
            "phone" to editTextPhone.text.toString()
        )

        // Update the user document in Firestore
        firestore.collection("users").document(currentUser.uid)
            .update(userMap as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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