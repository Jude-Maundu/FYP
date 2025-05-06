package com.example.fypapplication.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.FeedbackAdapter
import com.example.fypapplication.login.LoginActivity
import com.example.fypapplication.project.Feedback
import com.example.fypapplication.project.Project
import com.example.fypapplication.repository.FeedbackRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var textViewWelcome: TextView
    private lateinit var cardViewProjects: CardView
    private lateinit var cardViewSchedule: CardView
    private lateinit var cardViewSubmissions: CardView
    private lateinit var cardViewFeedback: CardView

    // Project summary views
    private lateinit var textViewProjectCount: TextView
    private lateinit var textViewRecentProject: TextView
    private lateinit var textViewProjectStatus: TextView
    private lateinit var btnViewProjects: Button

    // Submission summary views
    private lateinit var textViewSubmissionCount: TextView
    private lateinit var textViewRecentSubmission: TextView
    private lateinit var textViewSubmissionStatus: TextView

    // Feedback views and variables
    private lateinit var feedbackAdapter: FeedbackAdapter
    private lateinit var recyclerViewFeedback: RecyclerView
    private lateinit var textViewNoFeedback: TextView
    private lateinit var textViewFeedbackCount: TextView
    private val feedbackRepository = FeedbackRepository()
    private val feedbackList = mutableListOf<Feedback>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "StudentDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Set up the action bar
        supportActionBar?.title = getString(R.string.student_dashboard)

        // Initialize views
        textViewWelcome = findViewById(R.id.textViewWelcome)
        cardViewProjects = findViewById(R.id.cardViewProjects)
        cardViewSchedule = findViewById(R.id.cardViewSchedule)
        cardViewSubmissions = findViewById(R.id.cardViewSubmissions)
        cardViewFeedback = findViewById(R.id.cardViewFeedback)

        // Initialize project summary views
        textViewProjectCount = findViewById(R.id.textViewProjectCount)
        textViewRecentProject = findViewById(R.id.textViewRecentProject)
        textViewProjectStatus = findViewById(R.id.textViewProjectStatus)
        btnViewProjects = findViewById(R.id.btnViewProjects)

        // Initialize submission summary views
        textViewSubmissionCount = findViewById(R.id.textViewSubmissionCount)
        textViewRecentSubmission = findViewById(R.id.textViewRecentSubmission)
        textViewSubmissionStatus = findViewById(R.id.textViewSubmissionStatus)

        // Initialize feedback views
        try {
            recyclerViewFeedback = findViewById(R.id.recyclerViewFeedback)
            textViewNoFeedback = findViewById(R.id.textViewNoFeedback)
            textViewFeedbackCount = findViewById(R.id.textViewFeedbackCount)

            // Set up feedback section
            setupFeedbackSection()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing feedback views", e)
        }

        // Set up click listeners for dashboard cards
        setupCardListeners()

        // Load user data
        loadUserData()

        // Load project summary
        loadProjectSummary()

        // Load submission summary
        loadSubmissionSummary()
    }

    private fun setupFeedbackSection() {
        // Set up adapter
        feedbackAdapter = FeedbackAdapter(feedbackList) { feedback ->
            // Handle feedback item click - open detail view
            showFeedbackDetail(feedback)
        }

        recyclerViewFeedback.layoutManager = LinearLayoutManager(this)
        recyclerViewFeedback.adapter = feedbackAdapter

        // Load feedback
        loadFeedback()
    }

    private fun loadFeedback() {
        lifecycleScope.launch {
            try {
                val result = feedbackRepository.getStudentFeedback()

                if (result.isSuccess) {
                    val allFeedback = result.getOrNull() ?: emptyList()

                    // Update feedback count
                    textViewFeedbackCount.text = "Total: ${allFeedback.size}"

                    // Take only the most recent 3 feedback items for the dashboard
                    val recentFeedback = allFeedback.take(3)

                    feedbackList.clear()
                    feedbackList.addAll(recentFeedback)

                    if (feedbackList.isEmpty()) {
                        textViewNoFeedback.visibility = View.VISIBLE
                        recyclerViewFeedback.visibility = View.GONE
                    } else {
                        textViewNoFeedback.visibility = View.GONE
                        recyclerViewFeedback.visibility = View.VISIBLE
                        feedbackAdapter.notifyDataSetChanged()
                    }
                } else {
                    textViewNoFeedback.visibility = View.VISIBLE
                    recyclerViewFeedback.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading feedback", e)
                textViewNoFeedback.visibility = View.VISIBLE
                recyclerViewFeedback.visibility = View.GONE
            }
        }
    }

    private fun showFeedbackDetail(feedback: Feedback) {
        // Mark as read
        lifecycleScope.launch {
            feedbackRepository.markFeedbackAsRead(feedback.id)
        }

        // Show feedback detail dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Feedback from ${feedback.supervisorName}")
            .setMessage(feedback.content)
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun setupCardListeners() {
        cardViewProjects.setOnClickListener {
            // Navigate to projects screen
            startActivity(Intent(this, ProjectsListActivity::class.java))
        }

        cardViewSchedule.setOnClickListener {
            // Navigate to schedule screen
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        cardViewSubmissions.setOnClickListener {
            // Navigate to submissions screen - Update to use new submission system
            navigateToSubmissions()
        }

        cardViewFeedback.setOnClickListener {
            // Navigate to feedback list activity
            val intent = Intent(this, FeedbackListActivity::class.java)
            startActivity(intent)
        }

        // Set up view all projects button
        btnViewProjects.setOnClickListener {
            startActivity(Intent(this, ProjectsListActivity::class.java))
        }
    }

    private fun loadSubmissionSummary() {
        val currentUser = auth.currentUser ?: return

        // First, get the user's project ID
        firestore.collection("projects")
            .whereEqualTo("studentId", currentUser.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { projectDocuments ->
                if (projectDocuments.isEmpty) {
                    // No projects yet
                    textViewSubmissionCount.text = "Total: 0"
                    textViewRecentSubmission.text = "No submissions yet"
                    textViewSubmissionStatus.text = "Create a project first"
                    return@addOnSuccessListener
                }

                val projectId = projectDocuments.documents[0].id

                // Now get submissions for this project
                firestore.collection("submissions")
                    .whereEqualTo("projectId", projectId)
                    .orderBy("submissionDate", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        // Update submission count
                        textViewSubmissionCount.text = "Total: ${documents.size()}"

                        if (documents.isEmpty) {
                            textViewRecentSubmission.text = "No submissions yet"
                            textViewSubmissionStatus.text = "Submit your work"
                        } else {
                            val recentSubmission = documents.documents[0]
                            textViewRecentSubmission.text = recentSubmission.getString("title") ?: "Untitled"
                            val status = recentSubmission.getString("status") ?: "PENDING"
                            textViewSubmissionStatus.text = status

                            // Set color based on status
                            val statusColor = when (status) {
                                "PENDING" -> R.color.status_proposed
                                "REVIEWED" -> R.color.status_in_progress
                                "APPROVED" -> R.color.status_approved
                                "REJECTED" -> R.color.status_rejected
                                else -> R.color.status_proposed
                            }
                            textViewSubmissionStatus.setTextColor(getColor(statusColor))
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading submissions", e)
                        textViewRecentSubmission.text = "Error loading submissions"
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading project for submissions", e)
                textViewRecentSubmission.text = "Error loading project"
            }
    }

    private fun navigateToSubmissions() {
        // Check if user has any projects
        val currentUser = auth.currentUser ?: return

        firestore.collection("projects")
            .whereEqualTo("studentId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No projects, show message
                    Toast.makeText(this, "Create a project first before making submissions", Toast.LENGTH_SHORT).show()
                } else {
                    // Has projects, get the first/active project and navigate to submissions
                    val project = documents.documents[0].toObject(Project::class.java)
                    project?.let {
                        val intent = Intent(this, SubmissionListActivity::class.java).apply {
                            putExtra("PROJECT_ID", it.id)
                            putExtra("SUPERVISOR_ID", it.supervisorId)
                            putExtra("IS_STUDENT", true)
                        }
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking projects", e)
                Toast.makeText(this, "Error accessing projects", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProjectSummary() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("projects")
            .whereEqualTo("studentId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)  // Get only the most recent project
            .get()
            .addOnSuccessListener { documents ->
                // Get total project count
                firestore.collection("projects")
                    .whereEqualTo("studentId", currentUser.uid)
                    .count()
                    .get(AggregateSource.SERVER)
                    .addOnSuccessListener { count ->
                        textViewProjectCount.text = "Total: ${count.count}"
                    }

                if (documents.isEmpty) {
                    textViewRecentProject.text = "No projects yet"
                    textViewProjectStatus.text = "Create your first project"
                    textViewProjectStatus.setTextColor(getColor(R.color.status_proposed))
                } else {
                    val recentProject = documents.documents[0].toObject(Project::class.java)
                    recentProject?.let { project ->
                        textViewRecentProject.text = project.title
                        textViewProjectStatus.text = project.status.capitalize()

                        // Set color based on status
                        val statusColor = when (project.status) {
                            "proposed" -> R.color.status_proposed
                            "approved" -> R.color.status_approved
                            "in_progress" -> R.color.status_in_progress
                            "completed" -> R.color.status_completed
                            "rejected" -> R.color.status_rejected
                            else -> R.color.status_proposed
                        }
                        textViewProjectStatus.setTextColor(getColor(statusColor))
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle error
                textViewRecentProject.text = "Error loading projects"
                textViewProjectStatus.text = ""
                Log.e(TAG, "Error loading projects", e)
            }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Not logged in, redirect to login
            navigateToLogin()
            return
        }

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val displayName = when {
                    document == null || !document.exists() -> getString(R.string.welcome_student)
                    else -> {
                        val username = document.getString("username")
                        when {
                            username == null -> getString(R.string.student)
                            username.contains("@") -> username.substringBefore("@")
                            else -> username
                        }
                    }
                }
                textViewWelcome.text = getString(R.string.welcome_message, displayName)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user data", e)
                textViewWelcome.text = getString(R.string.welcome_student)
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh summaries when returning to dashboard
        loadProjectSummary()
        loadSubmissionSummary()

        // Reload feedback
        try {
            loadFeedback()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading feedback on resume", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                navigateToLogin()
                true
            }
            R.id.action_profile -> {
                // Navigate to profile screen
                startActivity(Intent(this, StudentProfileActivity::class.java))
                true
            }
            R.id.action_settings -> {
                // Navigate to settings screen
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}