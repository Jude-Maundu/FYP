package com.example.fypapplication.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Project
import java.text.SimpleDateFormat
import java.util.*

class ProjectAdapter(private val projects: List<Project>) :
    RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private var onItemClickListener: ((Project) -> Unit)? = null
    var onApproveClick: ((Project) -> Unit)? = null
    var onDeclineClick: ((Project) -> Unit)? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun setOnItemClickListener(listener: (Project) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.bind(project)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(project)
        }
    }

    override fun getItemCount(): Int = projects.size

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardProject: CardView = itemView.findViewById(R.id.cardProject)
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewProjectTitle)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewProjectStatus)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewProjectDate)
        private val textViewStudent: TextView = itemView.findViewById(R.id.textViewStudentName)

        private val buttonApprove: Button = itemView.findViewById(R.id.buttonApprove)
        private val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)

        fun bind(project: Project) {
            textViewTitle.text = project.title

            // Format and display creation date
            val formattedDate = project.createdAt?.let { dateFormat.format(it) } ?: "Unknown date"
            textViewDate.text = "Created: $formattedDate"

            // Set student name if available
            if (project.studentName.isNotEmpty()) {
                textViewStudent.text = "Student: ${project.studentName}"
                textViewStudent.visibility = View.VISIBLE
            } else {
                textViewStudent.visibility = View.GONE
            }

            // Set status styling
            val statusColor: Int
            val backgroundColor: Int

            when (project.status.lowercase()) {
                "approved" -> {
                    textViewStatus.text = "✓ Approved"
                    statusColor = Color.parseColor("#4CAF50")
                    backgroundColor = Color.parseColor("#E8F5E9")
                }
                "rejected" -> {
                    textViewStatus.text = "✗ Rejected"
                    statusColor = Color.parseColor("#F44336")
                    backgroundColor = Color.parseColor("#FFEBEE")
                }
                else -> {
                    textViewStatus.text = "⟳ Proposed"
                    statusColor = Color.parseColor("#FFA000")
                    backgroundColor = Color.parseColor("#FFF8E1")
                }
            }

            textViewStatus.setTextColor(statusColor)
            cardProject.setCardBackgroundColor(backgroundColor)

            // Show/Hide approve/decline buttons
            if (project.status.lowercase() == "proposed") {
                buttonApprove.visibility = View.VISIBLE
                buttonDecline.visibility = View.VISIBLE

                buttonApprove.setOnClickListener {
                    onApproveClick?.invoke(project)
                }

                buttonDecline.setOnClickListener {
                    onDeclineClick?.invoke(project)
                }
            } else {
                buttonApprove.visibility = View.GONE
                buttonDecline.visibility = View.GONE
            }
        }
    }
}
