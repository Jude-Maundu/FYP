package com.example.fypapplication.student

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.ProjectAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.fypapplication.project.Project

class ProjectsListActivity : AppCompatActivity() {

    private lateinit var recyclerViewProjects: RecyclerView
    private lateinit var textViewNoProjects: TextView
    private lateinit var btnCreateProject: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var projectAdapter: ProjectAdapter
    private val projectsList = mutableListOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects_list)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Projects"

        // Initialize views
        recyclerViewProjects = findViewById(R.id.recyclerViewProjects)
        textViewNoProjects = findViewById(R.id.textViewNoProjects)
        btnCreateProject = findViewById(R.id.btnCreateProject)

        // Set up RecyclerView with adapter
        setupRecyclerView()

        // Set up create project button
        btnCreateProject.setOnClickListener {
            startActivity(Intent(this, ProjectCreationActivity::class.java))
        }

        // Load projects
        loadProjects()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with empty list
        projectAdapter = ProjectAdapter(projectsList)

        // Set click listener
        projectAdapter.setOnItemClickListener { project ->
            // Handle project click - navigate to project details
            val intent = Intent(this, ProjectDetailActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            startActivity(intent)
        }

        // Set up recycler view
        recyclerViewProjects.apply {
            layoutManager = LinearLayoutManager(this@ProjectsListActivity)
            adapter = projectAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the projects list when returning to this screen
        loadProjects()
    }

    private fun loadProjects() {
        val currentUser = auth.currentUser ?: return

        // Client-side filtering approach that doesn't require index
        firestore.collection("projects")
            .get()
            .addOnSuccessListener { documents ->
                projectsList.clear()

                val userProjects = documents
                    .filter { it.getString("studentId") == currentUser.uid }
                    .sortedByDescending { (it.getTimestamp("createdAt")?.toDate()) }

                if (userProjects.isEmpty()) {
                    textViewNoProjects.visibility = View.VISIBLE
                    recyclerViewProjects.visibility = View.GONE
                } else {
                    textViewNoProjects.visibility = View.GONE
                    recyclerViewProjects.visibility = View.VISIBLE

                    for (document in userProjects) {
                        val project = document.toObject(Project::class.java)
                        // Make sure to set the document ID as the project ID
                        project.id = document.id
                        projectsList.add(project)
                    }

                    projectAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                // Handle error
                textViewNoProjects.text = "Error loading projects: ${e.message}"
                textViewNoProjects.visibility = View.VISIBLE
                recyclerViewProjects.visibility = View.GONE
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