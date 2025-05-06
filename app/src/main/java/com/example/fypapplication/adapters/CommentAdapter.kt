package com.example.fypapplication.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
        private val textViewUserRole: TextView = itemView.findViewById(R.id.textViewUserRole)
        private val textViewCommentText: TextView = itemView.findViewById(R.id.textViewCommentText)
        private val textViewCommentDate: TextView = itemView.findViewById(R.id.textViewCommentDate)

        fun bind(comment: Comment) {
            textViewUserName.text = comment.userName
            textViewUserRole.text = comment.userRole
            textViewCommentText.text = comment.commentText
            textViewCommentDate.text = formatDate(comment.commentDate)

            // Set role color
            val roleColor = if (comment.userRole == "SUPERVISOR") {
                Color.parseColor("#2196F3") // Blue for supervisor
            } else {
                Color.parseColor("#4CAF50") // Green for student
            }
            textViewUserRole.setTextColor(roleColor)
        }

        private fun formatDate(date: Date): String {
            val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            return dateFormat.format(date)
        }
    }
}