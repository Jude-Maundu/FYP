package com.example.fypapplication.student



import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.FileAttachmentAdapter
import com.example.fypapplication.project.Submission
import com.example.fypapplication.repository.SubmissionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

class SubmissionActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var spinnerSubmissionType: Spinner
    private lateinit var buttonAttachFiles: Button
    private lateinit var buttonSubmit: Button
    private lateinit var recyclerViewAttachments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewNoFiles: TextView

    private val selectedFiles = mutableListOf<Uri>()
    private lateinit var fileAdapter: FileAttachmentAdapter

    private val submissionRepository = SubmissionRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var projectId: String
    private lateinit var supervisorId: String

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                // Handle multiple files selection
                if (intent.clipData != null) {
                    val clipData = intent.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedFiles.add(uri)
                    }
                }
                // Handle single file selection
                else if (intent.data != null) {
                    val uri = intent.data!!
                    selectedFiles.add(uri)
                }

                updateAttachmentsList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission)

        // Get project details from intent
        projectId = intent.getStringExtra("PROJECT_ID") ?: ""
        supervisorId = intent.getStringExtra("SUPERVISOR_ID") ?: ""

        if (projectId.isEmpty() || supervisorId.isEmpty()) {
            Toast.makeText(this, "Project information missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        editTextTitle = findViewById(R.id.editTextSubmissionTitle)
        editTextDescription = findViewById(R.id.editTextSubmissionDescription)
        spinnerSubmissionType = findViewById(R.id.spinnerSubmissionType)
        buttonAttachFiles = findViewById(R.id.buttonAttachFiles)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        recyclerViewAttachments = findViewById(R.id.recyclerViewAttachments)
        progressBar = findViewById(R.id.progressBar)
        textViewNoFiles = findViewById(R.id.textViewNoFiles)

        // Set up submission type spinner
        setupSubmissionTypeSpinner()

        // Set up file attachments recycler view
        setupFileAttachmentsRecyclerView()

        // Set up attach files button
        buttonAttachFiles.setOnClickListener {
            openFilePicker()
        }

        // Set up submit button
        buttonSubmit.setOnClickListener {
            submitProject()
        }
    }

    private fun setupSubmissionTypeSpinner() {
        val submissionTypes = arrayOf(
            "Project Proposal",
            "Progress Report",
            "Final Report",
            "Presentation Slides",
            "Source Code",
            "Other"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, submissionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubmissionType.adapter = adapter
    }

    private fun setupFileAttachmentsRecyclerView() {
        fileAdapter = FileAttachmentAdapter(selectedFiles) { position ->
            // Remove file on click
            selectedFiles.removeAt(position)
            updateAttachmentsList()
        }

        recyclerViewAttachments.apply {
            layoutManager = LinearLayoutManager(this@SubmissionActivity)
            adapter = fileAdapter
        }

        updateAttachmentsList()
    }

    private fun updateAttachmentsList() {
        if (selectedFiles.isEmpty()) {
            textViewNoFiles.visibility = View.VISIBLE
            recyclerViewAttachments.visibility = View.GONE
        } else {
            textViewNoFiles.visibility = View.GONE
            recyclerViewAttachments.visibility = View.VISIBLE
            fileAdapter.notifyDataSetChanged()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"  // All file types
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        filePickerLauncher.launch(intent)
    }

    private fun submitProject() {
        val title = editTextTitle.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val submissionType = spinnerSubmissionType.selectedItem.toString()

        // Validate inputs
        if (title.isEmpty()) {
            editTextTitle.error = "Title is required"
            return
        }

        if (description.isEmpty()) {
            editTextDescription.error = "Description is required"
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE
        buttonSubmit.isEnabled = false

        // Create submission object
        val submission = Submission(
            projectId = projectId,
            studentId = auth.currentUser?.uid ?: "",
            supervisorId = supervisorId,
            title = title,
            description = description,
            submissionType = submissionType,
            submissionDate = Date(),
            status = "PENDING"
        )

        // Submit to Firebase
        lifecycleScope.launch {
            try {
                val result = submissionRepository.createSubmission(submission, selectedFiles)

                if (result.isSuccess) {
                    Toast.makeText(
                        this@SubmissionActivity,
                        "Submission successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@SubmissionActivity,
                        "Error: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SubmissionActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
                buttonSubmit.isEnabled = true
            }
        }
    }
}