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

        // Action bar
        supportActionBar?.title = getString(R.string.projects_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Views
        recyclerViewProjects = findViewById(R.id.recyclerViewProjects)
        textViewNoProjects = findViewById(R.id.textViewNoProjects)
        progressBar = findViewById(R.id.progressBar)

        // RecyclerView setup
        recyclerViewProjects.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(projectsList)

        adapter.setOnItemClickListener { project ->
            val intent = Intent(this, SupervisorProjectDetailActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            startActivity(intent)
        }

        // ✅ Approve/Decline buttons handling
        adapter.onApproveClick = { project ->
            updateProjectStatus(project.id, "Approved")
        }

        adapter.onDeclineClick = { project ->
            updateProjectStatus(project.id, "Rejected")
        }

        recyclerViewProjects.adapter = adapter

        // Load projects
        loadSupervisorProjects()
    }

    private fun loadSupervisorProjects() {
        progressBar.visibility = View.VISIBLE
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val supervisorId = currentUser.uid
            val supervisorEmail = currentUser.email

            firestore.collection("projects")
                .whereEqualTo("supervisorId", supervisorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        processProjects(snapshot.documents)
                    } else if (supervisorEmail != null) {
                        firestore.collection("projects")
                            .whereEqualTo("supervisorEmail", supervisorEmail)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { emailSnapshot ->
                                if (!emailSnapshot.isEmpty) {
                                    processProjects(emailSnapshot.documents)
                                } else {
                                    loadProjectsFromStudents(supervisorId, supervisorEmail)
                                }
                            }
                            .addOnFailureListener {
                                loadProjectsFromStudents(supervisorId, supervisorEmail)
                            }
                    } else {
                        loadProjectsFromStudents(supervisorId, null)
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    showNoProjectsMessage(getString(R.string.error_loading_projects))
                }
        } else {
            progressBar.visibility = View.GONE
            showNoProjectsMessage("You must be logged in to view projects")
        }
    }

    private fun loadProjectsFromStudents(supervisorId: String, supervisorEmail: String?) {
        firestore.collection("users")
            .whereEqualTo("supervisorId", supervisorId)
            .get()
            .addOnSuccessListener { studentsSnapshot ->
                val studentIds = studentsSnapshot.documents.map { it.id }

                if (studentIds.isNotEmpty()) {
                    firestore.collection("projects")
                        .whereIn("studentId", studentIds)
                        .get()
                        .addOnSuccessListener { projectsSnapshot ->
                            processProjects(projectsSnapshot.documents)
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            showNoProjectsMessage(getString(R.string.no_projects_found))
                        }
                } else if (supervisorEmail != null) {
                    firestore.collection("users")
                        .whereEqualTo("supervisorEmail", supervisorEmail)
                        .get()
                        .addOnSuccessListener { emailStudentsSnapshot ->
                            val emailStudentIds = emailStudentsSnapshot.documents.map { it.id }

                            if (emailStudentIds.isNotEmpty()) {
                                firestore.collection("projects")
                                    .whereIn("studentId", emailStudentIds)
                                    .get()
                                    .addOnSuccessListener { projectsSnapshot ->
                                        processProjects(projectsSnapshot.documents)
                                    }
                                    .addOnFailureListener {
                                        progressBar.visibility = View.GONE
                                        showNoProjectsMessage(getString(R.string.no_projects_found))
                                    }
                            } else {
                                progressBar.visibility = View.GONE
                                showNoProjectsMessage("No students assigned to you")
                            }
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            showNoProjectsMessage("No students found")
                        }
                } else {
                    progressBar.visibility = View.GONE
                    showNoProjectsMessage("No students assigned to you")
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showNoProjectsMessage("Error loading students: ${it.message}")
            }
    }

    private fun processProjects(projectDocuments: List<com.google.firebase.firestore.DocumentSnapshot>) {
        projectsList.clear()

        for (document in projectDocuments) {
            val project = document.toObject(Project::class.java)
            if (project != null) {
                if (project.id.isEmpty()) {
                    project.id = document.id
                }

                if (project.studentId.isNotEmpty()) {
                    firestore.collection("users").document(project.studentId)
                        .get()
                        .addOnSuccessListener { studentDoc ->
                            val studentName = studentDoc.getString("displayName")
                                ?: studentDoc.getString("name")
                                ?: studentDoc.getString("username")
                                ?: "Unknown Student"
                            project.studentName = studentName
                            addProjectToList(project)
                        }
                        .addOnFailureListener {
                            addProjectToList(project)
                        }
                } else {
                    addProjectToList(project)
                }
            }
        }

        if (projectDocuments.isEmpty()) {
            finishLoadingProjects()
        }
    }

    private fun addProjectToList(project: Project) {
        if (!projectsList.contains(project)) {
            projectsList.add(project)
            sortAndUpdateProjectsList()
        }
    }

    private fun sortAndUpdateProjectsList() {
        projectsList.sortWith(compareBy<Project> {
            when (it.status.lowercase()) {
                "proposed" -> 0
                "approved" -> 1
                "rejected" -> 2
                else -> 3
            }
        }.thenByDescending { it.createdAt })
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

    // ✅ Update project status in Firestore
    private fun updateProjectStatus(projectId: String, newStatus: String) {
        firestore.collection("projects").document(projectId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Log.d(tag, "Project $projectId updated to $newStatus")
                loadSupervisorProjects() // Refresh after update
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update project: ${e.message}")
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
