package com.example.fypapplication.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fypapplication.R
import com.example.fypapplication.adapters.SubmissionAdapter
import com.example.fypapplication.project.Submission
import com.example.fypapplication.repository.SubmissionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class SubmissionListActivity : AppCompatActivity(), View.OnLongClickListener {

    private lateinit var recyclerViewSubmissions: RecyclerView
    private lateinit var textViewNoSubmissions: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonNewSubmission: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var buttonDebug: Button

    private val submissionRepository = SubmissionRepository()
    private val submissionsList = mutableListOf<Submission>()
    private lateinit var submissionAdapter: SubmissionAdapter
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var projectId: String
    private lateinit var supervisorId: String
    private var isStudent = true

    private val TAG = "SubmissionListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_list)

        // Get project details from intent
        projectId = intent.getStringExtra("PROJECT_ID") ?: ""
        supervisorId = intent.getStringExtra("SUPERVISOR_ID") ?: ""
        isStudent = intent.getBooleanExtra("IS_STUDENT", true)

        Log.d(TAG, "onCreate - projectId: $projectId, supervisorId: $supervisorId, isStudent: $isStudent")

        if (projectId.isEmpty()) {
            Log.e(TAG, "Project ID is empty")
            Toast.makeText(this, "Error: Missing project information", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize views
        try {
            recyclerViewSubmissions = findViewById(R.id.recyclerViewSubmissions)
            textViewNoSubmissions = findViewById(R.id.textViewNoSubmissions)
            progressBar = findViewById(R.id.progressBar)
            buttonNewSubmission = findViewById(R.id.buttonNewSubmission)
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

            // Try to find the debug button if it exists in the layout
            try {
                buttonDebug = findViewById(R.id.buttonDebug)
                buttonDebug.setOnClickListener {
                    showDebugDialog()
                }

                // Make debug button visible on emulators
                if (isEmulator()) {
                    buttonDebug.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.d(TAG, "Debug button not found in layout", e)
                // Not critical, can continue without debug button
            }

            // Set up SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener {
                loadSubmissions()
            }

            // Setup long click listener for new submission button
            buttonNewSubmission.setOnLongClickListener(this)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup action bar
        supportActionBar?.title = "Project Submissions"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Verify user is authenticated
        if (auth.currentUser == null) {
            Log.e(TAG, "User is not authenticated")
            Toast.makeText(this, "You must be logged in to view submissions", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d(TAG, "Current user: ${auth.currentUser?.uid}")

        // Verify project exists
        verifyProject()

        // Setup new submission button (only visible for students)
        if (isStudent) {
            buttonNewSubmission.visibility = View.VISIBLE
            buttonNewSubmission.setOnClickListener {
                startNewSubmission()
            }
        } else {
            buttonNewSubmission.visibility = View.GONE
        }

        // Setup submissions recycler view
        setupSubmissionsRecyclerView()

        // Load submissions
        loadSubmissions()
    }

    private fun isEmulator(): Boolean {
        return android.os.Build.MODEL.contains("sdk", ignoreCase = true) ||
                android.os.Build.MODEL.contains("emulator", ignoreCase = true) ||
                android.os.Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                android.os.Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                android.os.Build.HARDWARE.contains("ranchu", ignoreCase = true)
    }

    private fun verifyProject() {
        lifecycleScope.launch {
            try {
                val projectDoc = firestore.collection("projects").document(projectId).get().await()
                if (!projectDoc.exists()) {
                    Log.e(TAG, "Project does not exist: $projectId")
                    showError("Project not found. Please try again.")
                    return@launch
                }

                // Get project details for logging
                val studentId = projectDoc.getString("studentId")
                val supervisorIdFromProject = projectDoc.getString("supervisorId")

                Log.d(TAG, "Project verified - studentId: $studentId, supervisorId: $supervisorIdFromProject")

                // Check if current user has access to this project
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != studentId && currentUserId != supervisorIdFromProject) {
                    Log.e(TAG, "User does not have access to this project")
                    showError("You don't have permission to access this project.")
                    return@launch
                }

                // Update supervisorId if it's different
                if (supervisorId.isEmpty() && supervisorIdFromProject != null) {
                    supervisorId = supervisorIdFromProject
                    Log.d(TAG, "Updated supervisorId from project: $supervisorId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying project", e)
                showError("Error verifying project: ${e.message}")
            }
        }
    }

    private fun setupSubmissionsRecyclerView() {
        try {
            submissionAdapter = SubmissionAdapter(submissionsList, isStudent) { submission ->
                // Handle item click - navigate to submission details
                val intent = Intent(this, SubmissionDetailActivity::class.java).apply {
                    putExtra("SUBMISSION_ID", submission.id)
                    putExtra("IS_STUDENT", isStudent)
                }
                startActivity(intent)
            }

            recyclerViewSubmissions.apply {
                layoutManager = LinearLayoutManager(this@SubmissionListActivity)
                adapter = submissionAdapter
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            Toast.makeText(this, "Error setting up view: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSubmissions() {
        // Only show progress bar if SwipeRefreshLayout is not refreshing
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }

        textViewNoSubmissions.visibility = View.GONE

        Log.d(TAG, "Loading submissions for project: $projectId")
        Log.d(TAG, "Current user ID: ${auth.currentUser?.uid ?: "Not authenticated"}")

        lifecycleScope.launch {
            try {
                // Check if user is still authenticated
                if (auth.currentUser == null) {
                    Log.e(TAG, "User is not authenticated")
                    showError("You must be logged in to view submissions.")
                    return@launch
                }

                // Log before repository call
                Log.d(TAG, "Calling repository.getSubmissionsForProject with projectId: $projectId")

                // Try a simpler query first for debugging
                try {
                    val testQuery = firestore.collection("submissions").limit(1).get().await()
                    Log.d(TAG, "Test query returned ${testQuery.documents.size} submissions")

                    if (testQuery.documents.isNotEmpty()) {
                        val firstDoc = testQuery.documents[0]
                        Log.d(TAG, "First submission in collection - ID: ${firstDoc.id}, projectId: ${firstDoc.getString("projectId")}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Test query failed", e)
                }

                // Now try to get submissions for this project
                val result = submissionRepository.getSubmissionsForProject(projectId)

                if (result.isSuccess) {
                    val submissions = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Successfully loaded ${submissions.size} submissions")

                    // Log first submission details if available
                    if (submissions.isNotEmpty()) {
                        Log.d(TAG, "First submission ID: ${submissions[0].id}")
                        Log.d(TAG, "First submission student ID: ${submissions[0].studentId}")
                    }

                    submissionsList.clear()
                    submissionsList.addAll(submissions)

                    if (submissionsList.isEmpty()) {
                        textViewNoSubmissions.text = "No submissions found. Create your first submission!"
                        textViewNoSubmissions.visibility = View.VISIBLE
                        swipeRefreshLayout.visibility = View.GONE
                    } else {
                        textViewNoSubmissions.visibility = View.GONE
                        swipeRefreshLayout.visibility = View.VISIBLE
                        submissionAdapter.notifyDataSetChanged()
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error loading submissions: ${error?.message}", error)

                    // More detailed error message
                    textViewNoSubmissions.text = when {
                        error is IllegalStateException -> "Authentication error: Please log in again."
                        error is SecurityException -> "Permission denied: You don't have access to these submissions."
                        error is IllegalArgumentException -> "Project error: ${error.message}"
                        error?.message?.contains("permission") == true -> "Permission denied: You don't have access to these submissions."
                        error?.message?.contains("NOT_FOUND") == true -> "Project not found or has no submissions."
                        else -> "Error loading submissions: ${error?.message ?: "Unknown error"}. Please try again later."
                    }
                    textViewNoSubmissions.visibility = View.VISIBLE
                    swipeRefreshLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading submissions", e)
                textViewNoSubmissions.text = "Error: ${e.message}"
                textViewNoSubmissions.visibility = View.VISIBLE
                swipeRefreshLayout.visibility = View.GONE
            } finally {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showError(message: String) {
        textViewNoSubmissions.text = message
        textViewNoSubmissions.visibility = View.VISIBLE
        swipeRefreshLayout.visibility = View.GONE
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        // Also show a toast for better visibility
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startNewSubmission() {
        if (projectId.isEmpty()) {
            Toast.makeText(this, "Project information missing. Please try again.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Get current project status
                val projectDoc = firestore.collection("projects").document(projectId).get().await()
                val projectStatus = projectDoc.getString("status") ?: ""

                if (projectStatus == "proposed") {
                    // Project is still in proposed status
                    AlertDialog.Builder(this@SubmissionListActivity)
                        .setTitle("Project Not Approved")
                        .setMessage("This project is still pending approval from a supervisor. You can create submissions after your project has been approved.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                // If supervisorId is empty, check if it's in the project document
                if (supervisorId.isEmpty()) {
                    supervisorId = projectDoc.getString("supervisorId") ?: ""
                    if (supervisorId.isEmpty()) {
                        Toast.makeText(this@SubmissionListActivity,
                            "This project doesn't have a supervisor assigned yet.",
                            Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                // Continue with creating new submission if project is approved and has a supervisor
                val intent = Intent(this@SubmissionListActivity, SubmissionActivity::class.java).apply {
                    putExtra("PROJECT_ID", projectId)
                    putExtra("SUPERVISOR_ID", supervisorId)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking project status", e)
                Toast.makeText(this@SubmissionListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTestSubmission() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val result = submissionRepository.createTestSubmission(projectId)

                if (result.isSuccess) {
                    Toast.makeText(this@SubmissionListActivity,
                        "Test submission created!",
                        Toast.LENGTH_SHORT).show()
                    loadSubmissions()
                } else {
                    val error = result.exceptionOrNull()
                    showError("Failed to create test submission: ${error?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test submission", e)
                showError("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDebugDialog() {
        val options = arrayOf(
            "Create Test Submission",
            "Check Project Existence",
            "View My User ID",
            "View Collection Counts",
            "Verify Permissions",
            "Print Project Data"
        )

        AlertDialog.Builder(this)
            .setTitle("Debug Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createTestSubmission()
                    1 -> verifyProject()
                    2 -> Toast.makeText(this, "User ID: ${auth.currentUser?.uid}", Toast.LENGTH_LONG).show()
                    3 -> checkCollectionCounts()
                    4 -> verifyPermissions()
                    5 -> printProjectData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun printProjectData() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                val projectDoc = firestore.collection("projects").document(projectId).get().await()

                if (projectDoc.exists()) {
                    val data = projectDoc.data
                    val stringBuilder = StringBuilder()

                    data?.forEach { (key, value) ->
                        stringBuilder.append("$key: $value\n")
                    }

                    AlertDialog.Builder(this@SubmissionListActivity)
                        .setTitle("Project Data")
                        .setMessage(stringBuilder.toString())
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(this@SubmissionListActivity,
                        "Project does not exist",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing project data", e)
                Toast.makeText(this@SubmissionListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun verifyPermissions() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                val currentUserId = auth.currentUser?.uid ?: "Not authenticated"
                val projectDoc = firestore.collection("projects").document(projectId).get().await()

                if (projectDoc.exists()) {
                    val studentId = projectDoc.getString("studentId") ?: "No student ID"
                    val supervisorId = projectDoc.getString("supervisorId") ?: "No supervisor ID"

                    val hasPermission = currentUserId == studentId || currentUserId == supervisorId

                    val message = """
                        Your ID: $currentUserId
                        Project ID: $projectId
                        Student ID: $studentId
                        Supervisor ID: $supervisorId
                        Has Permission: $hasPermission
                    """.trimIndent()

                    AlertDialog.Builder(this@SubmissionListActivity)
                        .setTitle("Permission Check")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(this@SubmissionListActivity,
                        "Project does not exist",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying permissions", e)
                Toast.makeText(this@SubmissionListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkCollectionCounts() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                // Check projects
                val projectsCount = firestore.collection("projects").get().await().documents.size

                // Check submissions
                val submissionsCount = firestore.collection("submissions").get().await().documents.size

                // Project-specific submissions
                val projectSubmissions = firestore.collection("submissions")
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()
                    .documents
                    .size

                // Show counts
                val message = """
                    Total Projects: $projectsCount
                    Total Submissions: $submissionsCount
                    This Project's Submissions: $projectSubmissions
                """.trimIndent()

                AlertDialog.Builder(this@SubmissionListActivity)
                    .setTitle("Collection Counts")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking collections", e)
                Toast.makeText(this@SubmissionListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload submissions when returning to this screen
        Log.d(TAG, "onResume - reloading submissions")
        loadSubmissions()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Add a long press listener to show debug options
    override fun onLongClick(v: View?): Boolean {
        if (v == buttonNewSubmission) {
            showDebugDialog()
            return true
        }
        return false
    }
}