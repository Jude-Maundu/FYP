package com.example.fypapplication.student

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.CommentAdapter
import com.example.fypapplication.adapters.SubmissionFileAdapter
import com.example.fypapplication.project.Comment
import com.example.fypapplication.project.Submission
import com.example.fypapplication.repository.SubmissionRepository
import com.example.fypapplication.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SubmissionDetailActivity : AppCompatActivity() {

    private lateinit var textViewTitle: TextView
    private lateinit var textViewType: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewSubmissionDate: TextView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewFeedback: TextView
    private lateinit var textViewFeedbackLabel: TextView
    private lateinit var textViewGrade: TextView
    private lateinit var textViewGradeLabel: TextView
    private lateinit var recyclerViewFiles: RecyclerView
    private lateinit var recyclerViewComments: RecyclerView
    private lateinit var editTextComment: EditText
    private lateinit var buttonAddComment: Button
    private lateinit var buttonApprove: Button
    private lateinit var buttonReject: Button
    private lateinit var linearLayoutReviewActions: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val submissionRepository = SubmissionRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var submissionId: String
    private var isStudent = true
    private lateinit var submission: Submission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_detail)

        // Get submission ID from intent
        submissionId = intent.getStringExtra("SUBMISSION_ID") ?: ""
        isStudent = intent.getBooleanExtra("IS_STUDENT", true)

        if (submissionId.isEmpty()) {
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Set up action bar
        supportActionBar?.title = "Submission Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Show/hide supervisor actions
        if (isStudent) {
            linearLayoutReviewActions.visibility = View.GONE
        } else {
            linearLayoutReviewActions.visibility = View.VISIBLE
        }

        // Load submission details
        loadSubmissionDetails()
    }

    private fun initializeViews() {
        textViewTitle = findViewById(R.id.textViewSubmissionTitle)
        textViewType = findViewById(R.id.textViewSubmissionType)
        textViewDescription = findViewById(R.id.textViewSubmissionDescription)
        textViewSubmissionDate = findViewById(R.id.textViewSubmissionDate)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewFeedback = findViewById(R.id.textViewFeedback)
        textViewFeedbackLabel = findViewById(R.id.textViewFeedbackLabel)
        textViewGrade = findViewById(R.id.textViewGrade)
        textViewGradeLabel = findViewById(R.id.textViewGradeLabel)
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles)
        recyclerViewComments = findViewById(R.id.recyclerViewComments)
        editTextComment = findViewById(R.id.editTextComment)
        buttonAddComment = findViewById(R.id.buttonAddComment)
        buttonApprove = findViewById(R.id.buttonApprove)
        buttonReject = findViewById(R.id.buttonReject)
        linearLayoutReviewActions = findViewById(R.id.linearLayoutReviewActions)
        progressBar = findViewById(R.id.progressBar)

        // Set up files recycler view
        recyclerViewFiles.layoutManager = LinearLayoutManager(this)

        // Set up comments recycler view
        recyclerViewComments.layoutManager = LinearLayoutManager(this)

        // Set up add comment button
        buttonAddComment.setOnClickListener {
            addComment()
        }

        // Set up approve and reject buttons
        buttonApprove.setOnClickListener {
            showFeedbackDialog("APPROVED")
        }

        buttonReject.setOnClickListener {
            showFeedbackDialog("REJECTED")
        }
    }

    private fun loadSubmissionDetails() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = submissionRepository.getSubmission(submissionId)

                if (result.isSuccess) {
                    submission = result.getOrNull()!!
                    displaySubmissionDetails(submission)
                } else {
                    showError("Error loading submission details")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displaySubmissionDetails(submission: Submission) {
        // Display basic details
        textViewTitle.text = submission.title
        textViewType.text = submission.submissionType
        textViewDescription.text = submission.description
        textViewSubmissionDate.text = formatDate(submission.submissionDate)

        // Set status with color
        textViewStatus.text = submission.status
        setStatusColor(submission.status)

        // Display feedback if available
        if (submission.feedback.isNotEmpty()) {
            textViewFeedbackLabel.visibility = View.VISIBLE
            textViewFeedback.visibility = View.VISIBLE
            textViewFeedback.text = submission.feedback
        } else {
            textViewFeedbackLabel.visibility = View.GONE
            textViewFeedback.visibility = View.GONE
        }

        // Display grade if available
        if (submission.grade.isNotEmpty()) {
            textViewGradeLabel.visibility = View.VISIBLE
            textViewGrade.visibility = View.VISIBLE
            textViewGrade.text = submission.grade
        } else {
            textViewGradeLabel.visibility = View.GONE
            textViewGrade.visibility = View.GONE
        }

        // Display files
        if (submission.fileUrls.isNotEmpty()) {
            val fileAdapter = SubmissionFileAdapter(submission.fileUrls) { fileUrl ->
                openFileUrl(fileUrl)
            }
            recyclerViewFiles.adapter = fileAdapter
        }

        // Display comments
        if (submission.comments.isNotEmpty()) {
            val commentAdapter = CommentAdapter(submission.comments)
            recyclerViewComments.adapter = commentAdapter
        }

        // Disable review buttons if already reviewed
        if (submission.status != "PENDING") {
            buttonApprove.isEnabled = false
            buttonReject.isEnabled = false
        }
    }

    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun setStatusColor(status: String) {
        val color = when (status) {
            "PENDING" -> Color.parseColor("#FFA000") // Orange
            "REVIEWED" -> Color.parseColor("#2196F3") // Blue
            "APPROVED" -> Color.parseColor("#4CAF50") // Green
            "REJECTED" -> Color.parseColor("#F44336") // Red
            else -> Color.parseColor("#757575") // Gray
        }
        textViewStatus.setTextColor(color)
    }

    private fun openFileUrl(fileUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(fileUrl)
        startActivity(intent)
    }

    private fun addComment() {
        val commentText = editTextComment.text.toString().trim()

        if (commentText.isEmpty()) {
            editTextComment.error = "Please enter a comment"
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Get user details
                    val userResult = userRepository.getUserProfile(currentUser.uid)
                    val userName = if (userResult.isSuccess) {
                        userResult.getOrNull()?.name ?: currentUser.displayName ?: "Anonymous"
                    } else {
                        currentUser.displayName ?: "Anonymous"
                    }

                    // Create comment
                    val comment = Comment(
                        id = UUID.randomUUID().toString(),
                        userId = currentUser.uid,
                        userName = userName,
                        userRole = if (isStudent) "STUDENT" else "SUPERVISOR",
                        commentText = commentText,
                        commentDate = Date()
                    )

                    // Add comment to submission
                    val result = submissionRepository.addComment(submissionId, comment)

                    if (result.isSuccess) {
                        // Clear comment field
                        editTextComment.text.clear()

                        // Reload submission details
                        loadSubmissionDetails()
                    } else {
                        showError("Error adding comment")
                    }
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showFeedbackDialog(status: String) {
        val feedbackView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val editTextFeedback = feedbackView.findViewById<EditText>(R.id.editTextFeedback)
        val editTextGrade = feedbackView.findViewById<EditText>(R.id.editTextGrade)

        AlertDialog.Builder(this)
            .setTitle(if (status == "APPROVED") "Approve Submission" else "Reject Submission")
            .setView(feedbackView)
            .setPositiveButton("Submit") { _, _ ->
                val feedback = editTextFeedback.text.toString().trim()
                val grade = editTextGrade.text.toString().trim()

                updateSubmissionStatus(status, feedback, grade)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSubmissionStatus(status: String, feedback: String, grade: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Update status and feedback
                val statusResult = submissionRepository.updateSubmissionStatus(
                    submissionId,
                    status,
                    feedback
                )

                // Update grade if provided
                if (grade.isNotEmpty() && statusResult.isSuccess) {
                    submissionRepository.gradeSubmission(submissionId, grade)
                }

                // Reload submission details
                loadSubmissionDetails()
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}