package com.example.fypapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Feedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackAdapter(
    private val feedbackList: List<Feedback>,
    private val onItemClick: (Feedback) -> Unit
) : RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]
        holder.bind(feedback)
    }

    override fun getItemCount(): Int = feedbackList.size

    inner class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewSupervisorName: TextView = itemView.findViewById(R.id.textViewSupervisorName)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewFeedbackDate)
        private val textViewContent: TextView = itemView.findViewById(R.id.textViewFeedbackContent)
        private val textViewUnread: TextView = itemView.findViewById(R.id.textViewUnreadIndicator)

        fun bind(feedback: Feedback) {
            textViewSupervisorName.text = feedback.supervisorName
            textViewDate.text = formatDate(feedback.createdAt)

            // Truncate content if it's too long
            val displayContent = if (feedback.content.length > 100) {
                "${feedback.content.substring(0, 97)}..."
            } else {
                feedback.content
            }
            textViewContent.text = displayContent

            // Show unread indicator if feedback hasn't been read
            textViewUnread.visibility = if (feedback.isRead) View.GONE else View.VISIBLE

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(feedback)
            }
        }

        private fun formatDate(date: Date): String {
            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return format.format(date)
        }
    }
}