package com.example.fypapplication.student

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.example.fypapplication.project.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ProjectCreationActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextObjectives: EditText
    private lateinit var btnSubmitProject: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_creation)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create New Project"

        // Initialize views
        editTextTitle = findViewById(R.id.editTextProjectTitle)
        editTextDescription = findViewById(R.id.editTextProjectDescription)
        editTextObjectives = findViewById(R.id.editTextProjectObjectives)
        btnSubmitProject = findViewById(R.id.btnSubmitProject)

        // Set up submit button
        btnSubmitProject.setOnClickListener {
            submitProject()
        }
    }

    private fun submitProject() {
        val title = editTextTitle.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val objectivesText = editTextObjectives.text.toString().trim()

        // Validation
        if (title.isEmpty() || description.isEmpty() || objectivesText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert objectives string to list (separated by new lines)
        val objectives = objectivesText.split("\n").filter { it.isNotEmpty() }

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to create a project", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Create project object
        val project = Project(
            id = "", // Firestore will generate this
            title = title,
            description = description,
            objectives = objectives,
            studentId = currentUser.uid,
            supervisorId = "", // Will be assigned later
            status = "proposed",
            createdAt = Date(),
            updatedAt = Date()
        )

        // Save to Firestore
        firestore.collection("projects")
            .add(project)
            .addOnSuccessListener { documentReference ->
                // Update the project with the generated ID
                firestore.collection("projects")
                    .document(documentReference.id)
                    .update("id", documentReference.id)

                Toast.makeText(this, "Project created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating project: ${e.message}", Toast.LENGTH_SHORT).show()
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