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
import com.example.fypapplication.adapters.StudentAdapter
import com.example.fypapplication.project.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SupervisorStudentsActivity : AppCompatActivity() {

    private val tag = "SupervisorStudents" // Changed to lowercase to fix warning
    private lateinit var recyclerViewStudents: RecyclerView
    private lateinit var textViewNoStudents: TextView
    private lateinit var progressBar: ProgressBar
    private val studentsList = ArrayList<Student>()
    private lateinit var adapter: StudentAdapter

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_students)

        // Set up action bar
        supportActionBar?.title = "My Students"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        recyclerViewStudents = findViewById(R.id.recyclerViewStudents)
        textViewNoStudents = findViewById(R.id.textViewNoStudents)
        progressBar = findViewById(R.id.progressBar)

        // Set up RecyclerView
        recyclerViewStudents.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(studentsList)
        adapter.setOnItemClickListener { student ->
            // Open student details when clicked
            val intent = Intent(this, StudentDetailsActivity::class.java)
            intent.putExtra("STUDENT_ID", student.id)
            startActivity(intent)
        }
        recyclerViewStudents.adapter = adapter

        // Load students data
        findSupervisorAndLoadStudents()
    }

    private fun findSupervisorAndLoadStudents() {
        progressBar.visibility = View.VISIBLE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userEmail = currentUser.email

            // Log all information for debugging
            Log.d(tag, "Current user ID: $userId, Email: $userEmail")

            // APPROACH 1: First try to find by email in supervisors collection
            if (!userEmail.isNullOrEmpty()) {
                firestore.collection("supervisors")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            // Found supervisor by email!
                            val supervisorDoc = snapshot.documents[0]
                            Log.d(tag, "Found supervisor by email: ${supervisorDoc.id}, data: ${supervisorDoc.data}")

                            // Load students for this supervisor
                            loadStudentsForSupervisor(supervisorDoc.id, supervisorDoc.getString("email"))
                        } else {
                            // APPROACH 2: Try finding supervisor by ID
                            tryFindSupervisorById(userId, userEmail)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error searching supervisors by email: ${e.message}")
                        tryFindSupervisorById(userId, userEmail)
                    }
            } else {
                // No email, try approach 2
                tryFindSupervisorById(userId, null)
            }
        } else {
            progressBar.visibility = View.GONE
            showNoStudentsMessage("You must be logged in to view students")
        }
    }

    private fun tryFindSupervisorById(userId: String, userEmail: String?) {
        Log.d(tag, "Trying to find supervisor by ID: $userId")

        // Try supervisors collection first
        firestore.collection("supervisors").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(tag, "Found supervisor in supervisors collection: ${document.data}")
                    // Found in supervisors collection
                    loadStudentsForSupervisor(userId, document.getString("email") ?: userEmail)
                } else {
                    // Try users collection with role=supervisor
                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists() && userDoc.getString("role") == "supervisor") {
                                Log.d(tag, "Found supervisor in users collection: ${userDoc.data}")
                                // Found in users collection
                                loadStudentsForSupervisor(userId, userDoc.getString("email") ?: userEmail)
                            } else {
                                // Last resort: search all users with this email and role=supervisor
                                if (!userEmail.isNullOrEmpty()) {
                                    firestore.collection("users")
                                        .whereEqualTo("email", userEmail)
                                        .whereEqualTo("role", "supervisor")
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            if (!snapshot.isEmpty) {
                                                val supervisorDoc = snapshot.documents[0]
                                                Log.d(tag, "Found supervisor by email in users: ${supervisorDoc.id}")
                                                loadStudentsForSupervisor(supervisorDoc.id, userEmail)
                                            } else {
                                                // No supervisor found
                                                progressBar.visibility = View.GONE
                                                showNoStudentsMessage("Account not recognized as supervisor. Please contact admin.")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(tag, "Error searching users by email: ${e.message}")
                                            progressBar.visibility = View.GONE
                                            showNoStudentsMessage("Error verifying supervisor: ${e.message}")
                                        }
                                } else {
                                    // No supervisor found
                                    progressBar.visibility = View.GONE
                                    showNoStudentsMessage("Account not recognized as supervisor. Please contact admin.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error checking users collection: ${e.message}")
                            progressBar.visibility = View.GONE
                            showNoStudentsMessage("Error verifying supervisor: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error checking supervisors collection: ${e.message}")
                progressBar.visibility = View.GONE
                showNoStudentsMessage("Error verifying supervisor: ${e.message}")
            }
    }

    private fun loadStudentsForSupervisor(supervisorId: String, supervisorEmail: String?) {
        Log.d(tag, "Loading students for supervisor ID: $supervisorId, Email: $supervisorEmail")

        // Approach 1: students with supervisorId field
        firestore.collection("users")
            .whereEqualTo("supervisorId", supervisorId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(tag, "Query 1 (supervisorId) returned ${snapshot.size()} results")

                if (!snapshot.isEmpty) {
                    processStudentsResult(snapshot.documents)
                } else if (supervisorEmail != null) {
                    // Approach 2: students with supervisorEmail field
                    firestore.collection("users")
                        .whereEqualTo("supervisorEmail", supervisorEmail)
                        .get()
                        .addOnSuccessListener { snapshot2 ->
                            Log.d(tag, "Query 2 (supervisorEmail) returned ${snapshot2.size()} results")

                            if (!snapshot2.isEmpty) {
                                processStudentsResult(snapshot2.documents)
                            } else {
                                // Approach 3: students with supervisor field (instead of supervisorId)
                                firestore.collection("users")
                                    .whereEqualTo("supervisor", supervisorId)
                                    .get()
                                    .addOnSuccessListener { snapshot3 ->
                                        Log.d(tag, "Query 3 (supervisor) returned ${snapshot3.size()} results")

                                        if (!snapshot3.isEmpty) {
                                            processStudentsResult(snapshot3.documents)
                                        } else {
                                            // No students found
                                            progressBar.visibility = View.GONE
                                            showNoStudentsMessage("No students assigned to you yet")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(tag, "Error in query 3: ${e.message}")
                                        progressBar.visibility = View.GONE
                                        showNoStudentsMessage("Error loading students: ${e.message}")
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error in query 2: ${e.message}")
                            progressBar.visibility = View.GONE
                            showNoStudentsMessage("Error loading students: ${e.message}")
                        }
                } else {
                    // No email to try, and first query failed
                    progressBar.visibility = View.GONE
                    showNoStudentsMessage("No students assigned to you yet")
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error in query 1: ${e.message}")
                progressBar.visibility = View.GONE
                showNoStudentsMessage("Error loading students: ${e.message}")
            }
    }

    private fun processStudentsResult(students: List<com.google.firebase.firestore.DocumentSnapshot>) {
        Log.d(tag, "Processing ${students.size} student documents")
        studentsList.clear()

        // Count how many students we need to process their project information
        var studentsToProcess = students.size

        if (studentsToProcess == 0) {
            progressBar.visibility = View.GONE
            showNoStudentsMessage("No students assigned to you yet")
            return
        }

        for (document in students) {
            try {
                val role = document.getString("role") ?: ""

                // Skip if the user is explicitly a supervisor (non-student)
                if (role.equals("supervisor", ignoreCase = true)) {
                    studentsToProcess--
                    if (studentsToProcess == 0) {
                        finishProcessingStudents()
                    }
                    continue
                }

                val name = document.getString("displayName")
                    ?: document.getString("name")
                    ?: document.getString("username")
                    ?: "Unknown Student"

                // Check for project ID
                val projectId = document.getString("projectId") ?: ""

                // Create the student with basic info
                val studentProjectTitle = document.getString("projectTitle") ?: "Not assigned"

                // Create student with mutable properties (var instead of val)
                val student = Student(
                    id = document.id,
                    name = name,
                    email = document.getString("email") ?: "",
                    phoneNumber = document.getString("phone") ?: "",
                    projectTitle = studentProjectTitle,
                    projectId = projectId,
                    department = document.getString("departmentId") ?: "",
                    profileImageUrl = document.getString("profileImageUrl") ?: ""
                )

                // If there's a project ID, try to get the actual project
                if (projectId.isNotEmpty()) {
                    firestore.collection("projects").document(projectId)
                        .get()
                        .addOnSuccessListener { projectDoc ->
                            if (projectDoc.exists()) {
                                // Create a new student object with updated project title
                                val updatedStudent = student.copy(
                                    projectTitle = projectDoc.getString("title") ?: student.projectTitle
                                )

                                // Add the updated student to the list
                                studentsList.add(updatedStudent)
                            } else {
                                // Project doesn't exist, add original student
                                studentsList.add(student)
                            }

                            studentsToProcess--
                            if (studentsToProcess == 0) {
                                finishProcessingStudents()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error getting project for student ${student.name}: ${e.message}")
                            // Still add the student even if project lookup fails
                            studentsList.add(student)
                            studentsToProcess--

                            if (studentsToProcess == 0) {
                                finishProcessingStudents()
                            }
                        }
                } else {
                    // No project ID, just add the student as is
                    studentsList.add(student)
                    studentsToProcess--

                    if (studentsToProcess == 0) {
                        finishProcessingStudents()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing student document: ${e.message}")
                studentsToProcess--

                if (studentsToProcess == 0) {
                    finishProcessingStudents()
                }
            }
        }
    }

    private fun finishProcessingStudents() {
        progressBar.visibility = View.GONE

        if (studentsList.isEmpty()) {
            showNoStudentsMessage("No students assigned to you yet")
        } else {
            Log.d(tag, "Displaying ${studentsList.size} students")
            textViewNoStudents.visibility = View.GONE
            recyclerViewStudents.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showNoStudentsMessage(message: String) {
        Log.d(tag, "Showing message: $message")
        textViewNoStudents.text = message
        textViewNoStudents.visibility = View.VISIBLE
        recyclerViewStudents.visibility = View.GONE
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