package com.example.fypapplication.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Submission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubmissionAdapter(
    private val submissions: List<Submission>,
    private val isStudent: Boolean,
    private val onItemClick: (Submission) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val submission = submissions[position]
        holder.bind(submission)
    }

    override fun getItemCount(): Int = submissions.size

    inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardViewSubmission)
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewSubmissionTitle)
        private val textViewType: TextView = itemView.findViewById(R.id.textViewSubmissionType)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewSubmissionDate)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewSubmissionStatus)
        private val textViewAttachments: TextView =
            itemView.findViewById(R.id.textViewAttachmentCount)

        fun bind(submission: Submission) {
            textViewTitle.text = submission.title
            textViewType.text = submission.submissionType
            textViewDate.text = formatDate(submission.submissionDate)

            // Set status with appropriate color
            textViewStatus.text = submission.status
            setStatusColor(submission.status)

            // Set attachment count
            val attachmentCount = submission.fileUrls.size
            textViewAttachments.text =
                "$attachmentCount ${if (attachmentCount == 1) "file" else "files"}"

            // Highlight items that need attention
            highlightIfNeeded(submission)

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(submission)
            }
        }

        private fun formatDate(date: Date): String {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return dateFormat.format(date)
        }

        private fun setStatusColor(status: String) {
            // Using Color.parseColor directly for now, can replace with toColorInt() extension if available
            val color = when (status) {
                "PENDING" -> Color.parseColor("#FFA000") // Orange
                "REVIEWED" -> Color.parseColor("#2196F3") // Blue
                "APPROVED" -> Color.parseColor("#4CAF50") // Green
                "REJECTED" -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#757575") // Gray
            }
            textViewStatus.setTextColor(color)
        }

        private fun highlightIfNeeded(submission: Submission) {
            // For students: highlight rejected submissions
            // For supervisors: highlight pending submissions
            val shouldHighlight = if (isStudent) {
                submission.status == "REJECTED"
            } else {
                submission.status == "PENDING"
            }

            if (shouldHighlight) {
                cardView.setCardBackgroundColor(Color.parseColor("#FFECB3")) // Light amber for highlighting
            } else {
                cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF")) // White for normal items
            }
        }
    }
}