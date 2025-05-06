package com.example.fypapplication.admin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminAssignSupervisorActivity : AppCompatActivity() {

    private val tag = "AdminAssignSupervisor"
    private lateinit var spinnerStudents: Spinner
    private lateinit var spinnerSupervisors: Spinner
    private lateinit var buttonAssignSupervisor: Button
    private lateinit var textViewCurrentSupervisor: TextView
    private lateinit var progressBar: ProgressBar

    private val firestore = FirebaseFirestore.getInstance()
    private val students = mutableListOf<StudentInfo>()
    private val supervisors = mutableListOf<SupervisorInfo>()

    private var selectedStudent: StudentInfo? = null
    private var selectedSupervisor: SupervisorInfo? = null
    private var currentSupervisorId: String? = null

    // Project supervisor updater
    private val projectUpdater = ProjectSupervisorUpdater()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_assign_supervisor)

        // Set up action bar
        supportActionBar?.title = "Assign Supervisor"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        spinnerStudents = findViewById(R.id.spinnerStudents)
        spinnerSupervisors = findViewById(R.id.spinnerSupervisors)
        buttonAssignSupervisor = findViewById(R.id.buttonAssignSupervisor)
        textViewCurrentSupervisor = findViewById(R.id.textViewCurrentSupervisor)
        progressBar = findViewById(R.id.progressBar)

        // Set up spinners
        setupSpinners()

        // Set up button click listener
        buttonAssignSupervisor.setOnClickListener {
            assignSupervisor()
        }

        // Load data
        loadStudents()
        loadSupervisors()
    }

    private fun setupSpinners() {
        // Set up student spinner
        val studentAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudents.adapter = studentAdapter

        spinnerStudents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < students.size) {
                    selectedStudent = students[position]
                    loadCurrentSupervisor(selectedStudent!!.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStudent = null
                textViewCurrentSupervisor.text = getString(R.string.current_supervisor_none)
            }
        }

        // Set up supervisor spinner
        val supervisorAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        supervisorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSupervisors.adapter = supervisorAdapter

        spinnerSupervisors.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSupervisor = if (position >= 0 && position < supervisors.size) {
                    supervisors[position]
                } else {
                    null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSupervisor = null
            }
        }
    }

    private fun loadStudents() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { querySnapshot ->
                students.clear()

                for (document in querySnapshot.documents) {
                    val id = document.id
                    val name = document.getString("displayName")
                        ?: document.getString("name")
                        ?: document.getString("username")
                        ?: getString(R.string.unknown_student)
                    val email = document.getString("email") ?: ""

                    students.add(StudentInfo(id, name, email))
                }

                updateStudentSpinner()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error loading students: ${e.message}")
                Toast.makeText(this, R.string.error_loading_students, Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun loadSupervisors() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("supervisors")
            .get()
            .addOnSuccessListener { querySnapshot ->
                supervisors.clear()

                for (document in querySnapshot.documents) {
                    val id = document.id
                    val name = document.getString("name") ?: getString(R.string.unknown_supervisor)
                    val email = document.getString("email") ?: ""
                    val currentStudents = document.getLong("currentStudents")?.toInt() ?: 0
                    val maxStudents = document.getLong("maxStudents")?.toInt() ?: 5

                    supervisors.add(SupervisorInfo(id, name, email, currentStudents, maxStudents))
                }

                // If no supervisors found in supervisors collection, check users collection
                if (supervisors.isEmpty()) {
                    loadSupervisorsFromUsers()
                } else {
                    updateSupervisorSpinner()
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error loading supervisors: ${e.message}")
                loadSupervisorsFromUsers() // Fallback to users collection
            }
    }

    private fun loadSupervisorsFromUsers() {
        firestore.collection("users")
            .whereEqualTo("role", "supervisor")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val id = document.id
                    val name = document.getString("displayName")
                        ?: document.getString("name")
                        ?: document.getString("username")
                        ?: getString(R.string.unknown_supervisor)
                    val email = document.getString("email") ?: ""

                    supervisors.add(SupervisorInfo(id, name, email, 0, 5))
                }

                updateSupervisorSpinner()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error loading supervisors from users: ${e.message}")
                Toast.makeText(this, R.string.error_loading_supervisors, Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun updateStudentSpinner() {
        val adapter = spinnerStudents.adapter as ArrayAdapter<String>
        adapter.clear()

        for (student in students) {
            adapter.add("${student.name} (${student.email})")
        }

        adapter.notifyDataSetChanged()
    }

    private fun updateSupervisorSpinner() {
        val adapter = spinnerSupervisors.adapter as ArrayAdapter<String>
        adapter.clear()

        for (supervisor in supervisors) {
            adapter.add("${supervisor.name} (${supervisor.currentStudents}/${supervisor.maxStudents})")
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadCurrentSupervisor(studentId: String) {
        firestore.collection("users")
            .document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentSupervisorId = document.getString("supervisorId")

                    if (currentSupervisorId != null && currentSupervisorId!!.isNotEmpty()) {
                        // Find supervisor name
                        getSupervisorName(currentSupervisorId!!) { name ->
                            textViewCurrentSupervisor.text = getString(R.string.current_supervisor_format, name)
                        }
                    } else {
                        textViewCurrentSupervisor.text = getString(R.string.current_supervisor_none)
                        currentSupervisorId = null
                    }
                } else {
                    textViewCurrentSupervisor.text = getString(R.string.current_supervisor_none)
                    currentSupervisorId = null
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error loading current supervisor: ${e.message}")
                textViewCurrentSupervisor.text = getString(R.string.current_supervisor_error)
                currentSupervisorId = null
            }
    }

    private fun getSupervisorName(supervisorId: String, callback: (String) -> Unit) {
        // First try supervisors collection
        firestore.collection("supervisors")
            .document(supervisorId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: getString(R.string.unknown_supervisor)
                    callback(name)
                } else {
                    // Try users collection
                    firestore.collection("users")
                        .document(supervisorId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val name = userDoc.getString("displayName")
                                    ?: userDoc.getString("name")
                                    ?: userDoc.getString("username")
                                    ?: getString(R.string.unknown_supervisor)

                                callback(name)
                            } else {
                                callback(getString(R.string.unknown_supervisor))
                            }
                        }
                        .addOnFailureListener {
                            callback(getString(R.string.unknown_supervisor))
                        }
                }
            }
            .addOnFailureListener {
                // Try users collection as fallback
                firestore.collection("users")
                    .document(supervisorId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            val name = userDoc.getString("displayName")
                                ?: userDoc.getString("name")
                                ?: userDoc.getString("username")
                                ?: getString(R.string.unknown_supervisor)

                            callback(name)
                        } else {
                            callback(getString(R.string.unknown_supervisor))
                        }
                    }
                    .addOnFailureListener {
                        callback(getString(R.string.unknown_supervisor))
                    }
            }
    }

    private fun assignSupervisor() {
        val student = selectedStudent
        val supervisor = selectedSupervisor

        if (student == null) {
            Toast.makeText(this, R.string.select_student_message, Toast.LENGTH_SHORT).show()
            return
        }

        if (supervisor == null) {
            Toast.makeText(this, R.string.select_supervisor_message, Toast.LENGTH_SHORT).show()
            return
        }

        // Check if supervisor has reached their maximum allowed students
        if (supervisor.currentStudents >= supervisor.maxStudents) {
            Toast.makeText(this, R.string.supervisor_max_students_message, Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Update the student's supervisorId field
        firestore.collection("users")
            .document(student.id)
            .update("supervisorId", supervisor.id)
            .addOnSuccessListener {
                // Update supervisor's currentStudents count if not already assigned
                if (currentSupervisorId != supervisor.id) {
                    updateSupervisorStudentCount(supervisor.id, 1)

                    // If there was a previous supervisor, decrease their count
                    if (currentSupervisorId != null) {
                        updateSupervisorStudentCount(currentSupervisorId!!, -1)
                    }
                }

                // Update all projects for this student with the new supervisor ID
                projectUpdater.updateProjectsWithSupervisor(student.id, supervisor.id) { success, message ->
                    if (success) {
                        Log.d(tag, "Successfully updated student projects: $message")
                    } else {
                        Log.e(tag, "Error updating student projects: $message")
                    }
                }

                Toast.makeText(this, R.string.supervisor_assigned_success, Toast.LENGTH_SHORT).show()

                // Refresh UI
                loadCurrentSupervisor(student.id)
                loadSupervisors() // Reload to update counts

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error assigning supervisor: ${e.message}")
                Toast.makeText(this, getString(R.string.error_assigning_supervisor, e.message), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun updateSupervisorStudentCount(supervisorId: String, change: Int) {
        firestore.collection("supervisors")
            .document(supervisorId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCount = document.getLong("currentStudents")?.toInt() ?: 0
                    val newCount = maxOf(0, currentCount + change)

                    firestore.collection("supervisors")
                        .document(supervisorId)
                        .update("currentStudents", newCount)
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error updating supervisor student count: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error getting supervisor document: ${e.message}")
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

    // Data classes for holding student and supervisor information
    data class StudentInfo(val id: String, val name: String, val email: String)

    data class SupervisorInfo(
        val id: String,
        val name: String,
        val email: String,
        val currentStudents: Int,
        val maxStudents: Int
    )
}