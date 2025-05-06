package com.example.fypapplication.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.adapters.MeetingAdapter
import com.example.fypapplication.project.Meeting
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class PastMeetingsFragment : Fragment() {

    private lateinit var recyclerViewMeetings: RecyclerView
    private lateinit var textViewNoMeetings: TextView
    private val meetingsList = mutableListOf<Meeting>()
    private lateinit var meetingAdapter: MeetingAdapter

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meetings, container, false)

        recyclerViewMeetings = view.findViewById(R.id.recyclerViewMeetings)
        textViewNoMeetings = view.findViewById(R.id.textViewNoMeetings)

        // Set up adapter
        meetingAdapter = MeetingAdapter(meetingsList) { meeting ->
            // Handle meeting click - navigate to meeting details
            (activity as? ScheduleActivity)?.showMeetingDetails(meeting)
        }

        recyclerViewMeetings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = meetingAdapter
        }

        loadPastMeetings()

        return view
    }

    // Changed to public to allow access from ScheduleActivity
    fun loadPastMeetings() {
        val currentUser = auth.currentUser ?: return

        // Get current date
        val currentDate = Date()

        firestore.collection("meetings")
            .whereEqualTo("studentId", currentUser.uid)
            .whereLessThan("meetingDate", currentDate)
            .orderBy("meetingDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                meetingsList.clear()

                if (documents.isEmpty) {
                    textViewNoMeetings.visibility = View.VISIBLE
                    textViewNoMeetings.text = "No past meetings"
                    recyclerViewMeetings.visibility = View.GONE
                } else {
                    textViewNoMeetings.visibility = View.GONE
                    recyclerViewMeetings.visibility = View.VISIBLE

                    for (document in documents) {
                        val meeting = document.toObject(Meeting::class.java)
                        meetingsList.add(meeting)
                    }

                    meetingAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                textViewNoMeetings.visibility = View.VISIBLE
                textViewNoMeetings.text = "Error loading meetings: ${e.message}"
                recyclerViewMeetings.visibility = View.GONE
            }
    }

    override fun onResume() {
        super.onResume()
        loadPastMeetings()
    }
}