package com.example.fypapplication.student

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.MilestoneAdapter
import com.example.fypapplication.project.Milestone
import com.example.fypapplication.project.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var textViewTitle: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewObjectives: TextView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewCreatedAt: TextView
    private lateinit var btnEditProject: ImageButton
    private lateinit var btnAddMilestone: Button
    private lateinit var recyclerViewMilestones: RecyclerView
    private lateinit var textViewNoMilestones: TextView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private lateinit var projectId: String
    private lateinit var project: Project
    private val milestones = mutableListOf<Milestone>()
    private lateinit var milestoneAdapter: MilestoneAdapter

    // Calendar for date picking
    private val calendar = Calendar.getInstance()
    private var selectedDueDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Project Details"

        // Initialize views
        textViewTitle = findViewById(R.id.textViewProjectTitle)
        textViewDescription = findViewById(R.id.textViewProjectDescription)
        textViewObjectives = findViewById(R.id.textViewProjectObjectives)
        textViewStatus = findViewById(R.id.textViewProjectStatus)
        textViewCreatedAt = findViewById(R.id.textViewProjectCreatedAt)
        btnEditProject = findViewById(R.id.btnEditProject)
        btnAddMilestone = findViewById(R.id.btnAddMilestone)
        recyclerViewMilestones = findViewById(R.id.recyclerViewMilestones)
        textViewNoMilestones = findViewById(R.id.textViewNoMilestones)

        // Get project ID from intent
        projectId = intent.getStringExtra("PROJECT_ID") ?: ""
        if (projectId.isEmpty()) {
            Toast.makeText(this, "Project ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up RecyclerView for milestones
        setupMilestonesRecyclerView()

        // Set up button click listeners
        btnEditProject.setOnClickListener {
            showEditProjectDialog()
        }

        btnAddMilestone.setOnClickListener {
            showAddMilestoneDialog()
        }

        // Load project details
        loadProjectDetails()
    }

    private fun setupMilestonesRecyclerView() {
        milestoneAdapter = MilestoneAdapter(
            milestones,
            { milestone -> showEditMilestoneDialog(milestone) },
            { milestone, isCompleted -> updateMilestoneCompletionStatus(milestone, isCompleted) }
        )

        recyclerViewMilestones.apply {
            layoutManager = LinearLayoutManager(this@ProjectDetailActivity)
            adapter = milestoneAdapter
        }
    }

    private fun loadProjectDetails() {
        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    project = document.toObject(Project::class.java)!!
                    displayProjectDetails(project)
                    loadProjectMilestones()
                } else {
                    // Project not found
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                // Handle error
                Toast.makeText(this, "Error loading project: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadProjectMilestones() {
        firestore.collection("milestones")
            .whereEqualTo("projectId", projectId)
            .get()
            .addOnSuccessListener { documents ->
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
                Toast.makeText(this, "Error loading milestones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayProjectDetails(project: Project) {
        textViewTitle.text = project.title
        textViewDescription.text = project.description

        // Format objectives as bullet points
        val objectivesText = project.objectives.joinToString(separator = "\n• ", prefix = "• ")
        textViewObjectives.text = objectivesText

        textViewStatus.text = project.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        textViewCreatedAt.text = "Created on: ${dateFormat.format(project.createdAt)}"

        // Set status color
        val statusColor = when (project.status) {
            "proposed" -> R.color.status_proposed
            "approved" -> R.color.status_approved
            "in_progress" -> R.color.status_in_progress
            "completed" -> R.color.status_completed
            "rejected" -> R.color.status_rejected
            else -> R.color.status_proposed
        }
        textViewStatus.setTextColor(getColor(statusColor))
    }

    private fun showEditProjectDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_project, null)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextProjectTitle)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextProjectDescription)
        val editTextObjectives = dialogView.findViewById<EditText>(R.id.editTextProjectObjectives)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelProjectEdit)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProjectEdit)

        // Pre-fill fields with current project data
        editTextTitle.setText(project.title)
        editTextDescription.setText(project.description)
        editTextObjectives.setText(project.objectives.joinToString(separator = "\n"))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val objectivesText = editTextObjectives.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert objectives string to list
            val objectives = objectivesText.split("\n").filter { it.isNotEmpty() }

            // Update project
            val updatedProject = project.copy(
                title = title,
                description = description,
                objectives = objectives,
                updatedAt = Date()
            )

            saveProjectChanges(updatedProject)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveProjectChanges(updatedProject: Project) {
        firestore.collection("projects").document(projectId)
            .set(updatedProject)
            .addOnSuccessListener {
                Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show()
                project = updatedProject
                displayProjectDetails(project)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating project: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddMilestoneDialog() {
        showMilestoneDialog(null)
    }

    private fun showEditMilestoneDialog(milestone: Milestone) {
        showMilestoneDialog(milestone)
    }

    private fun showMilestoneDialog(existingMilestone: Milestone?) {
        val isEdit = existingMilestone != null

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_milestone, null)
        val textViewDialogTitle = dialogView.findViewById<TextView>(R.id.textViewDialogTitle)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextMilestoneTitle)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMilestoneDescription)
        val btnSelectDueDate = dialogView.findViewById<Button>(R.id.btnSelectDueDate)
        val checkboxCompleted = dialogView.findViewById<CheckBox>(R.id.checkboxMilestoneCompleted)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelMilestone)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveMilestone)

        // Set dialog title based on mode
        textViewDialogTitle.text = if (isEdit) "Edit Milestone" else "Add Milestone"

        // Pre-fill fields if editing existing milestone
        if (isEdit) {
            editTextTitle.setText(existingMilestone!!.title)
            editTextDescription.setText(existingMilestone.description)
            selectedDueDate = existingMilestone.dueDate
            checkboxCompleted.isChecked = existingMilestone.isCompleted

            // Update button text to show date if available
            if (selectedDueDate != null) {
                btnSelectDueDate.text = dateFormat.format(selectedDueDate!!)
            }
        } else {
            selectedDueDate = null
        }

        // Set up date picker
        btnSelectDueDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedDueDate = calendar.time
                    btnSelectDueDate.text = dateFormat.format(selectedDueDate!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) {
                // Update existing milestone
                val updatedMilestone = existingMilestone!!.copy(
                    title = title,
                    description = description,
                    dueDate = selectedDueDate,
                    isCompleted = checkboxCompleted.isChecked,
                    completedDate = if (checkboxCompleted.isChecked && !existingMilestone.isCompleted) Date() else existingMilestone.completedDate
                )
                saveMilestoneChanges(updatedMilestone)
            } else {
                // Create new milestone
                val newMilestone = Milestone(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    title = title,
                    description = description,
                    dueDate = selectedDueDate,
                    isCompleted = checkboxCompleted.isChecked,
                    completedDate = if (checkboxCompleted.isChecked) Date() else null,
                    createdAt = Date()
                )
                saveMilestoneChanges(newMilestone)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveMilestoneChanges(milestone: Milestone) {
        firestore.collection("milestones").document(milestone.id)
            .set(milestone)
            .addOnSuccessListener {
                Toast.makeText(this, "Milestone saved", Toast.LENGTH_SHORT).show()
                loadProjectMilestones() // Reload all milestones
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving milestone: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMilestoneCompletionStatus(milestone: Milestone, isCompleted: Boolean) {
        val updatedMilestone = milestone.copy(
            isCompleted = isCompleted,
            completedDate = if (isCompleted && !milestone.isCompleted) Date() else milestone.completedDate
        )

        saveMilestoneChanges(updatedMilestone)
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