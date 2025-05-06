package com.example.fypapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Meeting
import java.text.SimpleDateFormat
import java.util.Locale

class MeetingAdapter(
    private val meetings: List<Meeting>,
    private val onMeetingClick: (Meeting) -> Unit
) : RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder>() {

    private val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    class MeetingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardViewMeeting)
        val textTitle: TextView = view.findViewById(R.id.textMeetingTitle)
        val textDateTime: TextView = view.findViewById(R.id.textMeetingDateTime)
        val textLocation: TextView = view.findViewById(R.id.textMeetingLocation)
        val textStatus: TextView = view.findViewById(R.id.textMeetingStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meeting, parent, false)
        return MeetingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        val meeting = meetings[position]

        holder.textTitle.text = meeting.title
        holder.textDateTime.text = dateFormat.format(meeting.meetingDate)
        holder.textLocation.text = meeting.location

        // Process status text and color
        val statusText = meeting.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        holder.textStatus.text = statusText

        val statusColor = when (meeting.status) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "cancelled" -> R.color.status_cancelled
            "completed" -> R.color.status_completed
            else -> R.color.status_pending
        }

        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, statusColor))

        // Handle click
        holder.cardView.setOnClickListener {
            onMeetingClick(meeting)
        }
    }

    override fun getItemCount() = meetings.size
}