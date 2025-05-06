package com.example.fypapplication.supervisor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.fypapplication.R
import com.example.fypapplication.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SupervisorDashboardActivity : AppCompatActivity() {

    private lateinit var textViewWelcome: TextView

    // Dashboard menu items
    private lateinit var cardStudents: CardView
    private lateinit var cardProjects: CardView
    private lateinit var cardEvaluations: CardView
    private lateinit var cardSchedule: CardView

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        // Set up action bar
        supportActionBar?.title = getString(R.string.supervisor_dashboard_title)

        // Initialize views
        textViewWelcome = findViewById(R.id.textViewWelcome)
        cardStudents = findViewById(R.id.cardStudents)
        cardProjects = findViewById(R.id.cardProjects)
        cardEvaluations = findViewById(R.id.cardEvaluations)
        cardSchedule = findViewById(R.id.cardSchedule)

        // Load and set welcome message
        loadUserProfile()

        // Set up click listeners for dashboard items
        setupDashboardListeners()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("supervisors").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("name") ?: getString(R.string.supervisor_default)
                        textViewWelcome.text = getString(R.string.welcome_message, username)
                    } else {
                        // Fallback to users collection if not found in supervisors
                        firestore.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { userDoc ->
                                val username = userDoc.getString("displayName")
                                    ?: userDoc.getString("username")
                                    ?: getString(R.string.supervisor_default)
                                textViewWelcome.text = getString(R.string.welcome_message, username)
                            }
                            .addOnFailureListener {
                                textViewWelcome.text = getString(R.string.welcome_message, getString(R.string.supervisor_default))
                            }
                    }
                }
                .addOnFailureListener {
                    textViewWelcome.text = getString(R.string.welcome_message, getString(R.string.supervisor_default))
                }
        }
    }

    private fun setupDashboardListeners() {
        cardStudents.setOnClickListener {
            // Navigate to the students activity
            startActivity(Intent(this, SupervisorStudentsActivity::class.java))
        }

        cardProjects.setOnClickListener {
            startActivity(Intent(this, SupervisorProjectsActivity::class.java))
        }

        cardEvaluations.setOnClickListener {
            Toast.makeText(this, getString(R.string.evaluations_coming_soon), Toast.LENGTH_SHORT).show()
        }

        cardSchedule.setOnClickListener {
            Toast.makeText(this, getString(R.string.schedule_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, SupervisorProfileActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SupervisorSettingsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                // Firebase logout
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}