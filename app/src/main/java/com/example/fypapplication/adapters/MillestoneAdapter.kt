package com.example.fypapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R
import com.example.fypapplication.project.Milestone
import java.text.SimpleDateFormat
import java.util.Locale

class MilestoneAdapter(
    private val milestones: List<Milestone>,
    private val onMilestoneClick: ((Milestone) -> Unit)? = null,
    private val onMilestoneCompleteToggle: ((Milestone, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class MilestoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkboxComplete: CheckBox = view.findViewById(R.id.checkboxMilestoneComplete)
        val textTitle: TextView = view.findViewById(R.id.textMilestoneTitle)
        val textDueDate: TextView = view.findViewById(R.id.textMilestoneDueDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MilestoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_milestone, parent, false)
        return MilestoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: MilestoneViewHolder, position: Int) {
        val milestone = milestones[position]

        holder.textTitle.text = milestone.title
        holder.checkboxComplete.isChecked = milestone.isCompleted

        // Format and display due date if available
        milestone.dueDate?.let {
            holder.textDueDate.text = "Due: ${dateFormat.format(it)}"
            holder.textDueDate.visibility = View.VISIBLE
        } ?: run {
            holder.textDueDate.visibility = View.GONE
        }

        // Handle checkbox toggling - only if callback is provided
        if (onMilestoneCompleteToggle != null) {
            holder.checkboxComplete.isEnabled = true
            holder.checkboxComplete.setOnCheckedChangeListener { _, isChecked ->
                onMilestoneCompleteToggle.invoke(milestone, isChecked)
            }
        } else {
            // Disable checkbox if no callback is provided
            holder.checkboxComplete.isEnabled = false
            holder.checkboxComplete.setOnCheckedChangeListener(null)
        }

        // Handle click on the entire item for editing - only if callback is provided
        if (onMilestoneClick != null) {
            holder.itemView.setOnClickListener {
                onMilestoneClick.invoke(milestone)
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount() = milestones.size
}