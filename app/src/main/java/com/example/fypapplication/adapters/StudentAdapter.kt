package com.example.fypapplication.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Student
import com.google.firebase.firestore.FirebaseFirestore

class StudentAdapter(private val students: List<Student>) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var onItemClickListener: ((Student) -> Unit)? = null
    private val firestore = FirebaseFirestore.getInstance()

    fun setOnItemClickListener(listener: (Student) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.bind(student)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(student)
        }
    }

    override fun getItemCount(): Int = students.size

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewStudentName)
        private val textViewEmail: TextView = itemView.findViewById(R.id.textViewStudentEmail)
        private val textViewProject: TextView = itemView.findViewById(R.id.textViewStudentProject)

        fun bind(student: Student) {
            textViewName.text = student.name
            textViewEmail.text = student.email

            // Check if student has a projectId rather than just a title
            if (student.projectId.isNotEmpty()) {
                // Fetch the project from Firestore
                firestore.collection("projects").document(student.projectId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val projectTitle = document.getString("title") ?: "Unnamed Project"
                            val projectStatus = document.getString("status") ?: "Proposed"

                            // Set project text with status
                            val statusText = when (projectStatus.lowercase()) {
                                "approved" -> "✓ Approved"
                                "rejected" -> "✗ Rejected"
                                else -> "⟳ Proposed"
                            }

                            // Set color based on status
                            when (projectStatus.lowercase()) {
                                "approved" -> textViewProject.setTextColor(Color.parseColor("#4CAF50")) // Green
                                "rejected" -> textViewProject.setTextColor(Color.parseColor("#F44336")) // Red
                                else -> textViewProject.setTextColor(Color.parseColor("#FFA000")) // Amber
                            }

                            textViewProject.text = "Project: $projectTitle ($statusText)"
                        } else {
                            textViewProject.text = "Project: Not found (ID: ${student.projectId})"
                        }
                    }
                    .addOnFailureListener {
                        textViewProject.text = "Project: Error loading"
                    }
            } else if (student.projectTitle.isNotEmpty() && student.projectTitle != "Not assigned") {
                // If we have a title but no ID, show the title
                textViewProject.text = "Project: ${student.projectTitle}"
            } else {
                // No project
                textViewProject.text = "Project: Not assigned"
            }
        }
    }
}