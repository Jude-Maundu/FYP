package com.example.fypapplication.student

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.fypapplication.R
import com.example.fypapplication.adapters.MeetingsPagerAdapter
import com.example.fypapplication.project.Meeting
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScheduleActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fabAddMeeting: FloatingActionButton

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Calendar for meeting date/time picker
    private val calendar = Calendar.getInstance()
    private var supervisorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Meeting Schedule"

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        fabAddMeeting = findViewById(R.id.fabAddMeeting)

        // Load supervisor ID
        loadSupervisorId()

        // Set up ViewPager and tabs
        setupViewPager()

        // Set up add meeting button
        fabAddMeeting.setOnClickListener {
            showAddMeetingDialog()
        }
    }

    private fun loadSupervisorId() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    supervisorId = document.getString("supervisorId") ?: ""

                    // Disable add meeting button if no supervisor is assigned
                    if (supervisorId.isEmpty()) {
                        fabAddMeeting.isEnabled = false
                        Toast.makeText(this, "No supervisor assigned yet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun setupViewPager() {
        val adapter = MeetingsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.upcoming)
                1 -> getString(R.string.past)
                else -> ""
            }
        }.attach()
    }

    fun showMeetingDetails(meeting: Meeting) {
        // Create a dialog to show meeting details
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_meeting_details, null)

        val textTitle = dialogView.findViewById<TextView>(R.id.textViewMeetingTitle)
        val textDateTime = dialogView.findViewById<TextView>(R.id.textViewMeetingDateTime)
        val textLocation = dialogView.findViewById<TextView>(R.id.textViewMeetingLocation)
        val textDescription = dialogView.findViewById<TextView>(R.id.textViewMeetingDescription)
        val textStatus = dialogView.findViewById<TextView>(R.id.textViewMeetingStatus)
        val textDuration = dialogView.findViewById<TextView>(R.id.textViewMeetingDuration)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseMeetingDetails)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelMeeting)

        // Fill in meeting details
        textTitle.text = meeting.title
        textDateTime.text = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            .format(meeting.meetingDate)
        textLocation.text = meeting.location
        textDescription.text = meeting.description.ifEmpty { "No description provided" }

        // Format status text
        val statusText = meeting.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        textStatus.text = statusText

        // Set status color
        val statusColor = when (meeting.status) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "cancelled" -> R.color.status_cancelled
            "completed" -> R.color.status_completed
            else -> R.color.status_pending
        }
        textStatus.setTextColor(getColor(statusColor))

        // Set duration text
        textDuration.text = "${meeting.duration} minutes"

        // Only show cancel button for upcoming meetings that aren't already cancelled
        val isPastMeeting = meeting.meetingDate.before(Date())
        val isCancellable = !isPastMeeting && meeting.status != "cancelled"
        btnCancel.visibility = if (isCancellable) android.view.View.VISIBLE else android.view.View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            // Ask for confirmation
            AlertDialog.Builder(this)
                .setTitle("Cancel Meeting")
                .setMessage("Are you sure you want to cancel this meeting?")
                .setPositiveButton("Yes") { _, _ ->
                    cancelMeeting(meeting)
                    dialog.dismiss()
                }
                .setNegativeButton("No", null)
                .show()
        }

        dialog.show()
    }

    private fun cancelMeeting(meeting: Meeting) {
        val updatedMeeting = meeting.copy(
            status = "cancelled",
            updatedAt = Date()
        )

        firestore.collection("meetings").document(meeting.id)
            .set(updatedMeeting)
            .addOnSuccessListener {
                Toast.makeText(this, "Meeting cancelled", Toast.LENGTH_SHORT).show()
                refreshMeetings()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cancelling meeting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshMeetings() {
        // Find current tab
        val currentItem = viewPager.currentItem

        // Get current fragment
        val fragment = supportFragmentManager.findFragmentByTag("f$currentItem")

        // Refresh the fragment
        if (fragment is UpcomingMeetingsFragment) {
            fragment.loadUpcomingMeetings()
        } else if (fragment is PastMeetingsFragment) {
            fragment.loadPastMeetings()
        }
    }

    private fun showAddMeetingDialog() {
        if (supervisorId.isEmpty()) {
            Toast.makeText(this, "No supervisor assigned yet", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_meeting, null)
        val textViewDialogTitle = dialogView.findViewById<TextView>(R.id.textViewDialogTitle)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextMeetingTitle)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMeetingDescription)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val editTextLocation = dialogView.findViewById<EditText>(R.id.editTextMeetingLocation)
        val spinnerDuration = dialogView.findViewById<Spinner>(R.id.spinnerDuration)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelMeeting)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveMeeting)

        textViewDialogTitle.text = getString(R.string.schedule_meeting)

        // Set default date and time to tomorrow at 10:00 AM
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)

        btnSelectDate.text = dateFormat.format(calendar.time)
        btnSelectTime.text = timeFormat.format(calendar.time)

        // Set up date picker
        btnSelectDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    btnSelectDate.text = dateFormat.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set minimum date to today
            val today = Calendar.getInstance()
            datePickerDialog.datePicker.minDate = today.timeInMillis

            datePickerDialog.show()
        }

        // Set up time picker
        btnSelectTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    btnSelectTime.text = timeFormat.format(calendar.time)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        // Set up spinner
        val durationValues = resources.getStringArray(R.array.meeting_durations)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durationValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapter
        spinnerDuration.setSelection(1) // Default to 30 minutes

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val location = editTextLocation.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (location.isEmpty()) {
                Toast.makeText(this, "Location cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse duration
            val durationString = spinnerDuration.selectedItem.toString()
            val duration = when {
                durationString.contains("15") -> 15
                durationString.contains("30") -> 30
                durationString.contains("45") -> 45
                durationString.contains("1 hour") -> 60
                durationString.contains("1.5") -> 90
                durationString.contains("2") -> 120
                else -> 30
            }

            // Create meeting
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to schedule a meeting", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            val meeting = Meeting(
                id = UUID.randomUUID().toString(),
                projectId = "", // Can be updated if we decide to link meetings to specific projects
                studentId = currentUser.uid,
                supervisorId = supervisorId,
                title = title,
                description = description,
                meetingDate = calendar.time,
                duration = duration,
                location = location,
                status = "pending", // Pending approval from supervisor
                createdAt = Date(),
                updatedAt = Date()
            )

            saveMeeting(meeting)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveMeeting(meeting: Meeting) {
        firestore.collection("meetings").document(meeting.id)
            .set(meeting)
            .addOnSuccessListener {
                Toast.makeText(this, "Meeting request sent", Toast.LENGTH_SHORT).show()

                // Switch to upcoming meetings tab and refresh
                viewPager.currentItem = 0
                refreshMeetings()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error scheduling meeting: ${e.message}", Toast.LENGTH_SHORT).show()
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