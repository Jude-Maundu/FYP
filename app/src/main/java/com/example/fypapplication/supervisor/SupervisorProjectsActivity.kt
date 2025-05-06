package com.example.fypapplication.supervisor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.ProjectAdapter
import com.example.fypapplication.project.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SupervisorProjectsActivity : AppCompatActivity() {

    private val tag = "SupervisorProjects"
    private lateinit var recyclerViewProjects: RecyclerView
    private lateinit var textViewNoProjects: TextView
    private lateinit var progressBar: ProgressBar
    private val projectsList = ArrayList<Project>()
    private lateinit var adapter: ProjectAdapter

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_projects)

        // Set up action bar
        supportActionBar?.title = getString(R.string.projects_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        recyclerViewProjects = findViewById(R.id.recyclerViewProjects)
        textViewNoProjects = findViewById(R.id.textViewNoProjects)
        progressBar = findViewById(R.id.progressBar)

        // Set up RecyclerView
        recyclerViewProjects.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(projectsList)
        adapter.setOnItemClickListener { project ->
            // Open project details when clicked
            val intent = Intent(this, SupervisorProjectDetailActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            startActivity(intent)
        }
        recyclerViewProjects.adapter = adapter

        // Load projects data
        loadSupervisorProjects()
    }

    private fun loadSupervisorProjects() {
        progressBar.visibility = View.VISIBLE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val supervisorId = currentUser.uid
            val supervisorEmail = currentUser.email

            Log.d(tag, "Loading projects for supervisor: $supervisorId, $supervisorEmail")

            // First look for projects where supervisorId matches
            firestore.collection("projects")
                .whereEqualTo("supervisorId", supervisorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        Log.d(tag, "Found ${snapshot.size()} projects by supervisorId")
                        processProjects(snapshot.documents)
                    } else if (supervisorEmail != null) {
                        // Try by supervisor email
                        firestore.collection("projects")
                            .whereEqualTo("supervisorEmail", supervisorEmail)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { emailSnapshot ->
                                if (!emailSnapshot.isEmpty) {
                                    Log.d(tag, "Found ${emailSnapshot.size()} projects by supervisorEmail")
                                    processProjects(emailSnapshot.documents)
                                } else {
                                    // Try by getting all students assigned to this supervisor
                                    loadProjectsFromStudents(supervisorId, supervisorEmail)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(tag, "Error getting projects by email: ${e.message}")
                                loadProjectsFromStudents(supervisorId, supervisorEmail)
                            }
                    } else {
                        loadProjectsFromStudents(supervisorId, null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error getting projects: ${e.message}")
                    progressBar.visibility = View.GONE
                    showNoProjectsMessage(getString(R.string.error_loading_projects))
                }
        } else {
            progressBar.visibility = View.GONE
            showNoProjectsMessage("You must be logged in to view projects")
        }
    }

    private fun loadProjectsFromStudents(supervisorId: String, supervisorEmail: String?) {
        Log.d(tag, "Looking for students assigned to supervisor")

        // Find all students assigned to this supervisor
        val query = firestore.collection("users")
            .whereEqualTo("supervisorId", supervisorId)

        query.get()
            .addOnSuccessListener { studentsSnapshot ->
                val studentIds = studentsSnapshot.documents.map { it.id }

                if (studentIds.isNotEmpty()) {
                    Log.d(tag, "Found ${studentIds.size} students assigned to supervisor")
                    // Get projects from these students
                    firestore.collection("projects")
                        .whereIn("studentId", studentIds)
                        .get()
                        .addOnSuccessListener { projectsSnapshot ->
                            Log.d(tag, "Found ${projectsSnapshot.size()} projects from students")
                            processProjects(projectsSnapshot.documents)
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error getting projects from students: ${e.message}")
                            progressBar.visibility = View.GONE
                            showNoProjectsMessage(getString(R.string.no_projects_found))
                        }
                } else if (supervisorEmail != null) {
                    // Try finding students by supervisor email
                    firestore.collection("users")
                        .whereEqualTo("supervisorEmail", supervisorEmail)
                        .get()
                        .addOnSuccessListener { emailStudentsSnapshot ->
                            val emailStudentIds = emailStudentsSnapshot.documents.map { it.id }

                            if (emailStudentIds.isNotEmpty()) {
                                Log.d(tag, "Found ${emailStudentIds.size} students by supervisor email")
                                firestore.collection("projects")
                                    .whereIn("studentId", emailStudentIds)
                                    .get()
                                    .addOnSuccessListener { projectsSnapshot ->
                                        processProjects(projectsSnapshot.documents)
                                    }
                                    .addOnFailureListener { e ->
                                        progressBar.visibility = View.GONE
                                        showNoProjectsMessage(getString(R.string.no_projects_found))
                                    }
                            } else {
                                progressBar.visibility = View.GONE
                                showNoProjectsMessage("No students assigned to you")
                            }
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            showNoProjectsMessage("No students found")
                        }
                } else {
                    progressBar.visibility = View.GONE
                    showNoProjectsMessage("No students assigned to you")
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                showNoProjectsMessage("Error loading students: ${e.message}")
            }
    }

    private fun processProjects(projectDocuments: List<com.google.firebase.firestore.DocumentSnapshot>) {
        projectsList.clear()

        for (document in projectDocuments) {
            try {
                val project = document.toObject(Project::class.java)
                if (project != null) {
                    // Ensure the project has an ID (just in case)
                    if (project.id.isEmpty()) {
                        project.id = document.id
                    }

                    // Get student name if available
                    if (project.studentId.isNotEmpty()) {
                        firestore.collection("users").document(project.studentId)
                            .get()
                            .addOnSuccessListener { studentDoc ->
                                if (studentDoc.exists()) {
                                    val studentName = studentDoc.getString("displayName")
                                        ?: studentDoc.getString("name")
                                        ?: studentDoc.getString("username")
                                        ?: "Unknown Student"

                                    project.studentName = studentName
                                }

                                addProjectToList(project)
                            }
                            .addOnFailureListener { e ->
                                Log.e(tag, "Error getting student info: ${e.message}")
                                addProjectToList(project)
                            }
                    } else {
                        addProjectToList(project)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing project: ${e.message}")
            }
        }

        // If there are no projects to process
        if (projectDocuments.isEmpty()) {
            finishLoadingProjects()
        }
    }

    private fun addProjectToList(project: Project) {
        // Add project if it's not already in the list
        if (!projectsList.contains(project)) {
            projectsList.add(project)

            // Sort and update after each addition
            sortAndUpdateProjectsList()
        }
    }

    private fun sortAndUpdateProjectsList() {
        // Sort projects by status (Proposed first) and then by creation date
        projectsList.sortWith(compareBy<Project> {
            when (it.status.lowercase()) {
                "proposed" -> 0
                "approved" -> 1
                "rejected" -> 2
                else -> 3
            }
        }.thenByDescending { it.createdAt })

        // Update UI
        finishLoadingProjects()
    }

    private fun finishLoadingProjects() {
        progressBar.visibility = View.GONE

        if (projectsList.isEmpty()) {
            showNoProjectsMessage(getString(R.string.no_projects_found))
        } else {
            textViewNoProjects.visibility = View.GONE
            recyclerViewProjects.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showNoProjectsMessage(message: String) {
        textViewNoProjects.text = message
        textViewNoProjects.visibility = View.VISIBLE
        recyclerViewProjects.visibility = View.GONE
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