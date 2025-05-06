package com.example.fypapplication.supervisor

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class StudentDetailsActivity : AppCompatActivity() {

    private val TAG = "StudentDetails"
    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhone: TextView
    private lateinit var textViewProject: TextView
    private lateinit var textViewProjectStatus: TextView
    private lateinit var textViewDepartment: TextView
    private lateinit var buttonApproveProject: Button
    private lateinit var buttonRejectProject: Button
    private lateinit var progressBar: ProgressBar

    private val firestore = FirebaseFirestore.getInstance()
    private var projectId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_details)

        // Set up action bar
        supportActionBar?.title = "Student Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        textViewName = findViewById(R.id.textViewStudentName)
        textViewEmail = findViewById(R.id.textViewStudentEmail)
        textViewPhone = findViewById(R.id.textViewStudentPhone)
        textViewProject = findViewById(R.id.textViewStudentProject)
        textViewProjectStatus = findViewById(R.id.textViewProjectStatus)
        textViewDepartment = findViewById(R.id.textViewStudentDepartment)
        buttonApproveProject = findViewById(R.id.buttonApproveProject)
        buttonRejectProject = findViewById(R.id.buttonRejectProject)
        progressBar = findViewById(R.id.progressBar)

        // Set initial button states
        buttonApproveProject.visibility = View.GONE
        buttonRejectProject.visibility = View.GONE

        // Get student ID from intent
        val studentId = intent.getStringExtra("STUDENT_ID")
        if (studentId != null) {
            loadStudentDetails(studentId)
        } else {
            Toast.makeText(this, "Student information not available", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up button click listeners
        buttonApproveProject.setOnClickListener {
            projectId?.let { id -> updateProjectStatus(id, "Approved") }
        }

        buttonRejectProject.setOnClickListener {
            showRejectionDialog()
        }
    }

    private fun loadStudentDetails(studentId: String) {
        progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Set basic student info
                    textViewName.text = document.getString("displayName")
                        ?: document.getString("name")
                                ?: document.getString("username")
                                ?: "N/A"
                    textViewEmail.text = document.getString("email") ?: "N/A"
                    textViewPhone.text = document.getString("phone") ?: "N/A"

                    // Get department info
                    val departmentId = document.getString("departmentId")
                    if (departmentId != null) {
                        loadDepartmentInfo(departmentId)
                    } else {
                        textViewDepartment.text = "Department: Not assigned"
                    }

                    // Get project info - first check for projectId
                    projectId = document.getString("projectId")

                    if (projectId != null && projectId!!.isNotEmpty()) {
                        loadProjectInfo(projectId!!)
                    } else {
                        // Check if there's a project title but no ID
                        val projectTitle = document.getString("projectTitle")
                        if (!projectTitle.isNullOrEmpty()) {
                            textViewProject.text = "Project: $projectTitle"
                            textViewProjectStatus.text = "Status: Unknown (No project ID)"
                            buttonApproveProject.visibility = View.GONE
                            buttonRejectProject.visibility = View.GONE
                        } else {
                            textViewProject.text = "Project: Not assigned"
                            textViewProjectStatus.text = "Status: N/A"
                            buttonApproveProject.visibility = View.GONE
                            buttonRejectProject.visibility = View.GONE
                        }
                        progressBar.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this, "Student data not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadDepartmentInfo(departmentId: String) {
        firestore.collection("departments").document(departmentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val departmentName = document.getString("name") ?: "Unknown Department"
                    textViewDepartment.text = "Department: $departmentName"
                } else {
                    textViewDepartment.text = "Department: Unknown"
                }
            }
            .addOnFailureListener {
                textViewDepartment.text = "Department: Unable to load"
            }
    }

    private fun loadProjectInfo(projectId: String) {
        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE

                if (document.exists()) {
                    val projectTitle = document.getString("title") ?: "Unnamed Project"
                    val projectStatus = document.getString("status") ?: "Proposed"

                    textViewProject.text = "Project: $projectTitle"
                    textViewProjectStatus.text = "Status: $projectStatus"

                    // Show/hide approval buttons based on status
                    if (projectStatus.equals("Proposed", ignoreCase = true)) {
                        buttonApproveProject.visibility = View.VISIBLE
                        buttonRejectProject.visibility = View.VISIBLE
                    } else {
                        buttonApproveProject.visibility = View.GONE
                        buttonRejectProject.visibility = View.GONE
                    }
                } else {
                    textViewProject.text = "Project: Not found (ID exists but no data)"
                    textViewProjectStatus.text = "Status: Unknown"
                    buttonApproveProject.visibility = View.GONE
                    buttonRejectProject.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                textViewProject.text = "Project: Error loading"
                textViewProjectStatus.text = "Status: Unknown"
                Log.e(TAG, "Error loading project", e)
            }
    }

    private fun updateProjectStatus(projectId: String, newStatus: String) {
        progressBar.visibility = View.VISIBLE

        firestore.collection("projects").document(projectId)
            .update("status", newStatus)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Project $newStatus", Toast.LENGTH_SHORT).show()

                // Refresh project info
                loadProjectInfo(projectId)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating project status", e)
            }
    }

    private fun showRejectionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reject Project")
            .setMessage("Are you sure you want to reject this project?")
            .setPositiveButton("Reject") { _, _ ->
                projectId?.let { id -> updateProjectStatus(id, "Rejected") }
            }
            .setNegativeButton("Cancel", null)
            .show()
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