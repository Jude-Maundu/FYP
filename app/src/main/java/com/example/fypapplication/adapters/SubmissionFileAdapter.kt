package com.example.fypapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R

class SubmissionFileAdapter(
    private val fileUrls: List<String>,
    private val onFileClick: (String) -> Unit
) : RecyclerView.Adapter<SubmissionFileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileUrl = fileUrls[position]
        holder.bind(fileUrl)
    }

    override fun getItemCount(): Int = fileUrls.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFileName: TextView = itemView.findViewById(R.id.textViewFileName)
        private val imageViewFileType: ImageView = itemView.findViewById(R.id.imageViewFileType)

        fun bind(fileUrl: String) {
            // Extract file name from URL
            val fileName = fileUrl.substringAfterLast('/', "File ${position + 1}")

            textViewFileName.text = fileName

            // Set file type icon based on extension
            val fileExtension = fileName.substringAfterLast('.', "").lowercase()
            setFileTypeIcon(fileExtension)

            // Set click listener to open file
            itemView.setOnClickListener {
                onFileClick(fileUrl)
            }
        }

        private fun setFileTypeIcon(extension: String) {
            val imageResource = when (extension) {
                "pdf" -> R.drawable.ic_file_pdf
                "doc", "docx" -> R.drawable.ic_file_word
                "xls", "xlsx" -> R.drawable.ic_file_excel
                "ppt", "pptx" -> R.drawable.ic_file_powerpoint
                "jpg", "jpeg", "png", "gif" -> R.drawable.ic_file_image
                "zip", "rar" -> R.drawable.ic_file_archive
                "txt" -> R.drawable.ic_file_text
                else -> R.drawable.ic_file_generic
            }

            imageViewFileType.setImageResource(imageResource)
        }
    }
}