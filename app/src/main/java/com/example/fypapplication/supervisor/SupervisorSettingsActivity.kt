package com.example.fypapplication.supervisor

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

class SupervisorSettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: Switch
    private lateinit var switchNotifications: Switch
    private lateinit var btnResetPassword: Button
    private lateinit var btnSaveSettings: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_settings)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // Initialize views
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        // Load settings
        loadUserSettings()

        // Set up reset password button
        btnResetPassword.setOnClickListener {
            resetPassword()
        }

        // Set up save button
        btnSaveSettings.setOnClickListener {
            saveUserSettings()
        }

        // Apply dark mode setting immediately
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            applyDarkModeSetting(isChecked)
        }
    }

    private fun loadUserSettings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Get user settings from Firestore
        firestore.collection("userSettings").document(currentUser.uid).get()
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

        // Save settings to Firestore
        firestore.collection("userSettings").document(currentUser.uid)
            .set(settingsMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                applyDarkModeSetting(switchDarkMode.isChecked)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetPassword() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        val email = currentUser.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "No email address associated with this account", Toast.LENGTH_SHORT).show()
            return
        }

        // Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error sending password reset: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyDarkModeSetting(darkModeEnabled: Boolean) {
        val mode = if (darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
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