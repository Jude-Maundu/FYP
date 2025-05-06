package com.example.fypapplication.student

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.fypapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: Switch
    private lateinit var switchNotifications: Switch
    private lateinit var buttonChangePassword: Button
    private lateinit var buttonSaveSettings: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Initialize views
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        buttonChangePassword = findViewById(R.id.buttonChangePassword)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)

        // Load user settings
        loadUserSettings()

        // Set up click listeners
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        buttonChangePassword.setOnClickListener {
            // Open password reset dialog
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.email != null) {
                auth.sendPasswordResetEmail(currentUser.email!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Password reset email sent to ${currentUser.email}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to send password reset email: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

        buttonSaveSettings.setOnClickListener {
            saveUserSettings()
        }
    }

    private fun loadUserSettings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        firestore.collection("user_settings").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val darkMode = document.getBoolean("darkMode") ?: false
                    val notifications = document.getBoolean("notifications") ?: true

                    switchDarkMode.isChecked = darkMode
                    switchNotifications.isChecked = notifications
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserSettings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        val settingsMap = hashMapOf(
            "darkMode" to switchDarkMode.isChecked,
            "notifications" to switchNotifications.isChecked
        )

        firestore.collection("user_settings").document(currentUser.uid)
            .set(settingsMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
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