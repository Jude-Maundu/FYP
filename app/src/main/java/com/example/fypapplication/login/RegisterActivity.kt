package com.example.fypapplication.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.fypapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var spinnerSchool: Spinner
    private lateinit var spinnerDepartment: Spinner
    private lateinit var buttonRegister: Button
    private lateinit var textViewLoginLink: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val schools = mutableListOf<SchoolItem>()
    private val departments = mutableListOf<DepartmentItem>()
    private val filteredDepartments = mutableListOf<DepartmentItem>()

    private lateinit var schoolAdapter: ArrayAdapter<String>
    private lateinit var departmentAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        spinnerSchool = findViewById(R.id.spinnerSchool)
        spinnerDepartment = findViewById(R.id.spinnerDepartment)
        buttonRegister = findViewById(R.id.buttonRegister)
        textViewLoginLink = findViewById(R.id.textViewLoginLink)
        progressBar = findViewById(R.id.progressBar)

        // Setup adapters with empty lists initially
        schoolAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        )
        schoolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSchool.adapter = schoolAdapter

        departmentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        )
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDepartment.adapter = departmentAdapter

        // Handle school selection change
        spinnerSchool.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < schools.size) {
                    updateDepartments(schools[position].id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        buttonRegister.setOnClickListener {
            registerUser()
        }

        textViewLoginLink.setOnClickListener {
            navigateToLogin()
        }

        // Load schools and departments
        loadSchoolsAndDepartments()
    }

    private fun loadSchoolsAndDepartments() {
        progressBar.visibility = View.VISIBLE
        Log.d("RegisterActivity", "Starting to load schools and departments")

        // Load schools
        firestore.collection("schools")
            .orderBy("name")
            .get()
            .addOnSuccessListener { schoolsSnapshot ->
                Log.d("RegisterActivity", "Schools loaded: ${schoolsSnapshot.size()} documents")
                schools.clear()
                for (doc in schoolsSnapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("name") ?: "Unknown School"
                    schools.add(SchoolItem(id, name))
                    Log.d("RegisterActivity", "Added school: $name with ID: $id")
                }

                // Update school spinner
                Log.d("RegisterActivity", "Updating school spinner with ${schools.size} schools")
                val schoolNames = schools.map { it.name }
                schoolAdapter.clear()
                schoolAdapter.addAll(schoolNames)
                schoolAdapter.notifyDataSetChanged()

                // Load departments
                firestore.collection("departments")
                    .get()
                    .addOnSuccessListener { departmentsSnapshot ->
                        Log.d("RegisterActivity", "Departments loaded: ${departmentsSnapshot.size()} documents")
                        departments.clear()
                        for (doc in departmentsSnapshot.documents) {
                            val id = doc.id
                            val name = doc.getString("name") ?: "Unknown Department"
                            val schoolId = doc.getString("schoolId") ?: ""
                            departments.add(DepartmentItem(id, name, schoolId))
                            Log.d("RegisterActivity", "Added department: $name with ID: $id for school: $schoolId")
                        }

                        // Check if we need to add default data
                        if (schools.isEmpty()) {
                            Log.d("RegisterActivity", "No schools found, adding defaults")
                            addDefaultData()
                        } else {
                            // Select first school and update departments
                            spinnerSchool.setSelection(0)
                            Log.d("RegisterActivity", "Selected first school: ${schools[0].name}")
                            updateDepartments(schools[0].id)
                        }

                        progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        Log.e("RegisterActivity", "Failed to load departments", e)
                        Toast.makeText(this, "Failed to load departments: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE

                        // If departments fail to load but we have schools
                        if (schools.isNotEmpty()) {
                            spinnerSchool.setSelection(0)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Failed to load schools", e)
                Toast.makeText(this, "Failed to load schools: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE

                // Add default data if loading fails
                addDefaultData()
            }
    }

    private fun addDefaultData() {
        Log.d("RegisterActivity", "Adding default schools and departments")

        // Add default schools
        val defaultSchools = listOf(
            mapOf("name" to "School of Computing"),
            mapOf("name" to "School of Business"),
            mapOf("name" to "School of Engineering")
        )

        for ((index, school) in defaultSchools.withIndex()) {
            val schoolName = school["name"] as String
            firestore.collection("schools").add(school)
                .addOnSuccessListener { docRef ->
                    Log.d("RegisterActivity", "Added default school: $schoolName with ID: ${docRef.id}")
                    val schoolItem = SchoolItem(docRef.id, schoolName)
                    schools.add(schoolItem)

                    // Update adapter
                    val schoolNames = schools.map { it.name }
                    schoolAdapter.clear()
                    schoolAdapter.addAll(schoolNames)
                    schoolAdapter.notifyDataSetChanged()

                    // Add default departments for this school
                    if (schoolName == "School of Computing") {
                        val defaultDepts = listOf(
                            mapOf("name" to "Computer Science", "schoolId" to docRef.id),
                            mapOf("name" to "Information Technology", "schoolId" to docRef.id)
                        )

                        for (dept in defaultDepts) {
                            val deptName = dept["name"] as String
                            firestore.collection("departments").add(dept)
                                .addOnSuccessListener { deptRef ->
                                    Log.d("RegisterActivity", "Added default department: $deptName with ID: ${deptRef.id}")
                                    departments.add(DepartmentItem(
                                        deptRef.id,
                                        deptName,
                                        dept["schoolId"] as String
                                    ))

                                    // If this is the first school and we've added its departments, update the department spinner
                                    if (index == 0) {
                                        spinnerSchool.setSelection(0)
                                        updateDepartments(docRef.id)
                                    }
                                }
                        }
                    } else if (schoolName == "School of Business") {
                        val defaultDepts = listOf(
                            mapOf("name" to "Accounting", "schoolId" to docRef.id),
                            mapOf("name" to "Marketing", "schoolId" to docRef.id)
                        )

                        for (dept in defaultDepts) {
                            val deptName = dept["name"] as String
                            firestore.collection("departments").add(dept)
                                .addOnSuccessListener { deptRef ->
                                    Log.d("RegisterActivity", "Added default department: $deptName with ID: ${deptRef.id}")
                                    departments.add(DepartmentItem(
                                        deptRef.id,
                                        deptName,
                                        dept["schoolId"] as String
                                    ))
                                }
                        }
                    } else if (schoolName == "School of Engineering") {
                        val defaultDepts = listOf(
                            mapOf("name" to "Civil Engineering", "schoolId" to docRef.id),
                            mapOf("name" to "Mechanical Engineering", "schoolId" to docRef.id)
                        )

                        for (dept in defaultDepts) {
                            val deptName = dept["name"] as String
                            firestore.collection("departments").add(dept)
                                .addOnSuccessListener { deptRef ->
                                    Log.d("RegisterActivity", "Added default department: $deptName with ID: ${deptRef.id}")
                                    departments.add(DepartmentItem(
                                        deptRef.id,
                                        deptName,
                                        dept["schoolId"] as String
                                    ))
                                }
                        }
                    }

                    // Select the first school if this is the first one
                    if (index == 0) {
                        spinnerSchool.setSelection(0)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RegisterActivity", "Failed to add default school: $schoolName", e)
                }
        }
    }

    private fun updateDepartments(schoolId: String) {
        filteredDepartments.clear()
        filteredDepartments.addAll(departments.filter { it.schoolId == schoolId })

        Log.d("RegisterActivity", "Updating departments for school ID: $schoolId, found ${filteredDepartments.size} departments")

        // Update adapter
        departmentAdapter.clear()
        departmentAdapter.addAll(filteredDepartments.map { it.name })
        departmentAdapter.notifyDataSetChanged()

        // Reset selection
        if (filteredDepartments.isNotEmpty()) {
            spinnerDepartment.setSelection(0)
            Log.d("RegisterActivity", "Selected first department: ${filteredDepartments[0].name}")
        } else {
            Log.d("RegisterActivity", "No departments available for selected school")
        }
    }

    private fun registerUser() {
        val username = editTextUsername.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        // Validate selected school and department
        val schoolPosition = spinnerSchool.selectedItemPosition
        val departmentPosition = spinnerDepartment.selectedItemPosition

        if (schoolPosition < 0 || schoolPosition >= schools.size) {
            Toast.makeText(this, "Please select a school", Toast.LENGTH_SHORT).show()
            return
        }

        if (departmentPosition < 0 || departmentPosition >= filteredDepartments.size) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedSchool = schools[schoolPosition]
        val selectedDepartment = filteredDepartments[departmentPosition]

        // Basic validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.valid_email), Toast.LENGTH_SHORT).show()
            return
        }

        buttonRegister.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Create user account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    // Automatically assign supervisor based on school and department
                    assignSupervisor(selectedSchool.id, selectedDepartment.id) { supervisorId, supervisorName ->
                        val userData = hashMapOf(
                            "uid" to uid,
                            "username" to username,
                            "email" to email,
                            "role" to "student",
                            "schoolId" to selectedSchool.id,
                            "departmentId" to selectedDepartment.id,
                            "supervisorId" to supervisorId,
                            "supervisorName" to supervisorName,  // Store supervisor name for quick reference
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("users")
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("RegisterActivity", "User data saved to Firestore")

                                // Show assignment info to user
                                if (supervisorId.isNotEmpty()) {
                                    Toast.makeText(
                                        this,
                                        "Successfully registered! Assigned to supervisor: $supervisorName",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Update supervisor's student count
                                    updateSupervisorStudentCount(supervisorId)
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Successfully registered! A supervisor will be assigned later.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                auth.signOut()
                                navigateToLogin(success = true)
                            }
                            .addOnFailureListener { e ->
                                Log.e("RegisterActivity", "Failed to save user data", e)
                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                buttonRegister.isEnabled = true
                                progressBar.visibility = View.GONE
                            }
                    }
                } else {
                    Log.e("RegisterActivity", "Registration failed", task.exception)
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    buttonRegister.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }
    }

    /**
     * Function to automatically assign a supervisor when a student registers
     * based on the selected school and department.
     */
    private fun assignSupervisor(schoolId: String, departmentId: String, callback: (String, String) -> Unit) {
        progressBar.visibility = View.VISIBLE
        Log.d("RegisterActivity", "Finding supervisor for department ID: $departmentId")

        // Criteria for supervisor assignment:
        // 1. Same department, least number of students, below max capacity
        // 2. Same school, least number of students, below max capacity
        // 3. Any supervisor with capacity
        // 4. Return empty if no suitable supervisor found

        // First try: Same department
        firestore.collection("supervisors")
            .whereEqualTo("departmentId", departmentId)
            .whereEqualTo("active", true)  // Only consider active supervisors
            .orderBy("currentStudents", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { departmentSupervisors ->
                if (!departmentSupervisors.isEmpty) {
                    // Try to find a supervisor with capacity in the department
                    for (doc in departmentSupervisors.documents) {
                        val supervisorId = doc.id
                        val name = doc.getString("name") ?: "Unknown Supervisor"
                        val maxStudents = doc.getLong("maxStudents")?.toInt() ?: 5
                        val currentStudents = doc.getLong("currentStudents")?.toInt() ?: 0
                        val specialization = doc.getString("specialization") ?: ""

                        Log.d("RegisterActivity", "Checking supervisor: $name ($supervisorId) with $currentStudents/$maxStudents students")

                        if (currentStudents < maxStudents) {
                            Log.d("RegisterActivity", "Found suitable supervisor in same department: $name")
                            callback(supervisorId, name)
                            return@addOnSuccessListener
                        }
                    }
                }

                // No suitable supervisor in department, try same school
                Log.d("RegisterActivity", "No suitable supervisor in department, trying school")
                findSupervisorInSchool(schoolId, departmentId, callback)
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Error finding supervisor in department", e)
                findSupervisorInSchool(schoolId, departmentId, callback)
            }
    }

    /**
     * Helper function to find a supervisor in the same school
     */
    private fun findSupervisorInSchool(schoolId: String, departmentId: String, callback: (String, String) -> Unit) {
        firestore.collection("supervisors")
            .whereEqualTo("schoolId", schoolId)
            .whereNotEqualTo("departmentId", departmentId)  // Exclude already checked department
            .whereEqualTo("active", true)
            .orderBy("departmentId", Query.Direction.ASCENDING)  // Group by department
            .orderBy("currentStudents", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { schoolSupervisors ->
                if (!schoolSupervisors.isEmpty) {
                    // Try to find a supervisor with capacity in the school
                    for (doc in schoolSupervisors.documents) {
                        val supervisorId = doc.id
                        val name = doc.getString("name") ?: "Unknown Supervisor"
                        val maxStudents = doc.getLong("maxStudents")?.toInt() ?: 5
                        val currentStudents = doc.getLong("currentStudents")?.toInt() ?: 0

                        if (currentStudents < maxStudents) {
                            Log.d("RegisterActivity", "Found suitable supervisor in same school: $name")
                            callback(supervisorId, name)
                            return@addOnSuccessListener
                        }
                    }
                }

                // No suitable supervisor in school, try any supervisor
                Log.d("RegisterActivity", "No suitable supervisor in school, trying any supervisor")
                findAnySupervisor(callback)
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Error finding supervisor in school", e)
                findAnySupervisor(callback)
            }
    }

    /**
     * Helper function to find any available supervisor
     */
    private fun findAnySupervisor(callback: (String, String) -> Unit) {
        firestore.collection("supervisors")
            .whereEqualTo("active", true)
            .orderBy("currentStudents", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { supervisors ->
                if (!supervisors.isEmpty) {
                    val doc = supervisors.documents[0]
                    val supervisorId = doc.id
                    val name = doc.getString("name") ?: "Unknown Supervisor"
                    val maxStudents = doc.getLong("maxStudents")?.toInt() ?: 5
                    val currentStudents = doc.getLong("currentStudents")?.toInt() ?: 0

                    if (currentStudents < maxStudents) {
                        Log.d("RegisterActivity", "Found any suitable supervisor: $name")
                        callback(supervisorId, name)
                    } else {
                        Log.d("RegisterActivity", "No supervisor with capacity found")
                        Toast.makeText(
                            this@RegisterActivity,
                            "No supervisors available with capacity. One will be assigned later.",
                            Toast.LENGTH_LONG
                        ).show()
                        callback("", "")
                    }
                } else {
                    Log.d("RegisterActivity", "No supervisors found")
                    Toast.makeText(
                        this@RegisterActivity,
                        "No supervisors found. One will be assigned later.",
                        Toast.LENGTH_LONG
                    ).show()
                    callback("", "")
                }
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Error finding any supervisor", e)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error finding supervisor: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                callback("", "")
            }
    }

    /**
     * Update the student count for a supervisor
     */
    private fun updateSupervisorStudentCount(supervisorId: String) {
        if (supervisorId.isEmpty()) return

        Log.d("RegisterActivity", "Updating student count for supervisor: $supervisorId")

        // Use a transaction to safely update the count
        firestore.runTransaction { transaction ->
            val supervisorRef = firestore.collection("supervisors").document(supervisorId)
            val snapshot = transaction.get(supervisorRef)
            val currentStudents = snapshot.getLong("currentStudents")?.toInt() ?: 0

            transaction.update(supervisorRef, "currentStudents", currentStudents + 1)

            // Return success value (not used)
            null
        }.addOnSuccessListener {
            Log.d("RegisterActivity", "Student count updated successfully")
        }.addOnFailureListener { e ->
            Log.e("RegisterActivity", "Failed to update student count", e)
        }
    }

    private fun navigateToLogin(success: Boolean = false) {
        Log.d("RegisterActivity", "Navigating to login, success: $success")

        val intent = Intent(this, LoginActivity::class.java)
        if (success) {
            intent.putExtra("registered", true)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // Data classes for school and department items
    data class SchoolItem(val id: String, val name: String)
    data class DepartmentItem(val id: String, val name: String, val schoolId: String)
}