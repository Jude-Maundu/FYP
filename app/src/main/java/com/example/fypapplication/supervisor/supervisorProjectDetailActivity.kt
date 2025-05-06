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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.MilestoneAdapter
import com.example.fypapplication.project.Milestone
import com.example.fypapplication.project.Project
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SupervisorProjectDetailActivity : AppCompatActivity() {

    private val tag = "SupervisorProjectDetail"
    private lateinit var textViewTitle: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewDate: TextView
    private lateinit var textViewStudent: TextView
    private lateinit var textViewObjectives: TextView
    private lateinit var recyclerViewMilestones: RecyclerView
    private lateinit var textViewNoMilestones: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonApprove: Button
    private lateinit var buttonReject: Button

    private val firestore = FirebaseFirestore.getInstance()
    private var projectId: String? = null
    private var studentId: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val milestones = mutableListOf<Milestone>()
    private lateinit var milestoneAdapter: MilestoneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_project_detail)

        // Set up action bar
        supportActionBar?.title = getString(R.string.project_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        textViewTitle = findViewById(R.id.textViewProjectTitle)
        textViewDescription = findViewById(R.id.textViewProjectDescription)
        // Fixed: Changed to match the ID in layout XML
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewDate = findViewById(R.id.textViewProjectDate)
        textViewStudent = findViewById(R.id.textViewStudentName)
        textViewObjectives = findViewById(R.id.textViewProjectObjectives)
        recyclerViewMilestones = findViewById(R.id.recyclerViewMilestones)
        textViewNoMilestones = findViewById(R.id.textViewNoMilestones)
        progressBar = findViewById(R.id.progressBar)
        buttonApprove = findViewById(R.id.buttonApproveProject)
        buttonReject = findViewById(R.id.buttonRejectProject)

        // Set up RecyclerView for milestones
        setupMilestonesRecyclerView()

        // Get project ID from intent
        projectId = intent.getStringExtra("PROJECT_ID")
        if (projectId != null) {
            loadProjectDetails(projectId!!)
        } else {
            Toast.makeText(this, "Project information not available", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up button listeners
        buttonApprove.setOnClickListener {
            updateProjectStatus("approved")
        }

        buttonReject.setOnClickListener {
            showRejectionDialog()
        }
    }

    private fun setupMilestonesRecyclerView() {
        milestoneAdapter = MilestoneAdapter(
            milestones,
            { milestone -> /* View-only for supervisor */ },
            null // Supervisor cannot mark milestones as complete
        )

        recyclerViewMilestones.apply {
            layoutManager = LinearLayoutManager(this@SupervisorProjectDetailActivity)
            adapter = milestoneAdapter
        }
    }

    private fun loadProjectDetails(projectId: String) {
        progressBar.visibility = View.VISIBLE

        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Extract project information
                    val project = document.toObject(Project::class.java)
                    if (project != null) {
                        displayProjectDetails(project)
                        studentId = project.studentId

                        // Get the student information if available
                        if (studentId != null && studentId!!.isNotEmpty()) {
                            loadStudentInfo(studentId!!)
                        } else {
                            textViewStudent.text = "Student: Not assigned"
                        }

                        // Load milestones
                        loadProjectMilestones(projectId)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error parsing project data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading project: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(tag, "Error loading project", e)
                finish()
            }
    }

    private fun loadProjectMilestones(projectId: String) {
        firestore.collection("milestones")
            .whereEqualTo("projectId", projectId)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                milestones.clear()

                if (documents.isEmpty) {
                    textViewNoMilestones.visibility = View.VISIBLE
                    recyclerViewMilestones.visibility = View.GONE
                } else {
                    textViewNoMilestones.visibility = View.GONE
                    recyclerViewMilestones.visibility = View.VISIBLE

                    for (document in documents) {
                        val milestone = document.toObject(Milestone::class.java)
                        milestones.add(milestone)
                    }

                    // Sort milestones by due date (null dates at the end)
                    milestones.sortWith(compareBy<Milestone> { it.isCompleted }
                        .thenBy { it.dueDate == null }
                        .thenBy { it.dueDate })

                    milestoneAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(tag, "Error loading milestones", e)
                // Don't show error toast as milestones are optional
            }
    }

    private fun displayProjectDetails(project: Project) {
        textViewTitle.text = project.title
        textViewDescription.text = project.description

        // Format and set the creation date
        val formattedDate = dateFormat.format(project.createdAt)
        textViewDate.text = "Created on: $formattedDate"

        // Format objectives as bullet points
        val objectivesText = if (project.objectives.isNotEmpty()) {
            project.objectives.joinToString(separator = "\n• ", prefix = "• ")
        } else {
            "No objectives specified"
        }
        textViewObjectives.text = objectivesText

        // Update status display
        updateStatusDisplay(project.status)

        // Show/hide approval buttons based on status
        if (project.status.equals("proposed", ignoreCase = true)) {
            buttonApprove.visibility = View.VISIBLE
            buttonReject.visibility = View.VISIBLE
        } else {
            buttonApprove.visibility = View.GONE
            buttonReject.visibility = View.GONE
        }
    }

    private fun loadStudentInfo(studentId: String) {
        firestore.collection("users").document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val studentName = document.getString("displayName")
                        ?: document.getString("name")
                        ?: document.getString("username")
                        ?: "Unknown Student"

                    textViewStudent.text = "Student: $studentName"
                } else {
                    textViewStudent.text = "Student: Not found"
                }
            }
            .addOnFailureListener { e ->
                textViewStudent.text = "Student: Error loading"
                Log.e(tag, "Error loading student info", e)
            }
    }

    private fun updateStatusDisplay(status: String) {
        // Set status with appropriate styling
        when (status.lowercase()) {
            "approved" -> {
                textViewStatus.text = "Status: ✓ Approved"
                textViewStatus.setTextColor(resources.getColor(R.color.status_approved, theme))
            }
            "rejected" -> {
                textViewStatus.text = "Status: ✗ Rejected"
                textViewStatus.setTextColor(resources.getColor(R.color.status_rejected, theme))
            }
            else -> { // proposed or any other status
                textViewStatus.text = "Status: ⟳ Proposed"
                textViewStatus.setTextColor(resources.getColor(R.color.status_proposed, theme))
            }
        }
    }

    private fun updateProjectStatus(newStatus: String) {
        if (projectId == null) {
            Toast.makeText(this, "Project ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        firestore.collection("projects").document(projectId!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE

                Toast.makeText(
                    this,
                    if (newStatus == "approved") getString(R.string.project_approved) else getString(R.string.project_rejected),
                    Toast.LENGTH_SHORT
                ).show()

                // Update the UI to reflect the new status
                updateStatusDisplay(newStatus)

                // Hide the approval buttons
                buttonApprove.visibility = View.GONE
                buttonReject.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, getString(R.string.error_updating_project), Toast.LENGTH_SHORT).show()
                Log.e(tag, "Error updating project status", e)
            }
    }

    private fun showRejectionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_reject_title))
            .setMessage(getString(R.string.confirm_reject_message))
            .setPositiveButton(getString(R.string.reject)) { _, _ ->
                updateProjectStatus("rejected")
            }
            .setNegativeButton(android.R.string.cancel, null)
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