package com.example.fypapplication.student

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.FeedbackAdapter
import com.example.fypapplication.project.Feedback
import com.example.fypapplication.repository.FeedbackRepository
import kotlinx.coroutines.launch

class FeedbackListActivity : AppCompatActivity() {

    private lateinit var recyclerViewFeedback: RecyclerView
    private lateinit var textViewNoFeedback: TextView
    private lateinit var progressBar: ProgressBar

    private val feedbackRepository = FeedbackRepository()
    private val feedbackList = mutableListOf<Feedback>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    private val tag = "FeedbackListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_list)

        // Set up ActionBar
        supportActionBar?.title = "Supervisor Feedback"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        recyclerViewFeedback = findViewById(R.id.recyclerViewFeedback)
        textViewNoFeedback = findViewById(R.id.textViewNoFeedback)
        progressBar = findViewById(R.id.progressBar)

        // Set up RecyclerView
        setupRecyclerView()

        // Load feedback
        loadFeedback()
    }

    private fun setupRecyclerView() {
        feedbackAdapter = FeedbackAdapter(feedbackList) { feedback ->
            showFeedbackDetail(feedback)
        }

        recyclerViewFeedback.apply {
            layoutManager = LinearLayoutManager(this@FeedbackListActivity)
            adapter = feedbackAdapter
        }
    }

    private fun loadFeedback() {
        progressBar.visibility = View.VISIBLE
        textViewNoFeedback.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Check if user is authenticated first
                val currentUser = feedbackRepository.getCurrentUser()
                if (currentUser == null) {
                    textViewNoFeedback.text = "Error: User not logged in"
                    textViewNoFeedback.visibility = View.VISIBLE
                    recyclerViewFeedback.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    return@launch
                }

                val result = feedbackRepository.getStudentFeedback()

                if (result.isSuccess) {
                    // Use a different variable name to avoid val reassignment
                    val receivedFeedback = result.getOrNull() ?: emptyList()

                    // Get only supervisor feedback
                    val supervisorFeedback = receivedFeedback.filter {
                        it.supervisorId.isNotEmpty()
                    }

                    // Update UI with the filtered list
                    updateFeedbackList(supervisorFeedback)
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error is java.net.UnknownHostException -> "No internet connection"
                        error?.message?.contains("permission") == true -> "Permission denied to access feedback"
                        else -> "Error loading feedback: ${error?.message}"
                    }

                    Log.e(tag, errorMessage, error)
                    textViewNoFeedback.text = errorMessage
                    textViewNoFeedback.visibility = View.VISIBLE
                    recyclerViewFeedback.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception loading feedback", e)
                textViewNoFeedback.text = "Error: ${e.message ?: "Unknown error"}"
                textViewNoFeedback.visibility = View.VISIBLE
                recyclerViewFeedback.visibility = View.GONE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateFeedbackList(newFeedbackList: List<Feedback>) {
        feedbackList.clear()
        feedbackList.addAll(newFeedbackList)

        if (feedbackList.isEmpty()) {
            textViewNoFeedback.text = "No supervisor feedback available yet"
            textViewNoFeedback.visibility = View.VISIBLE
            recyclerViewFeedback.visibility = View.GONE
        } else {
            textViewNoFeedback.visibility = View.GONE
            recyclerViewFeedback.visibility = View.VISIBLE
            feedbackAdapter.notifyDataSetChanged()
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

        // Update read status in our local list
        val index = feedbackList.indexOfFirst { it.id == feedback.id }
        if (index != -1) {
            val updatedFeedback = feedbackList[index].copy(isRead = true)
            feedbackList[index] = updatedFeedback
            feedbackAdapter.notifyItemChanged(index)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Using finish instead of onBackPressed to avoid errors
        finish()
        return true
    }
}