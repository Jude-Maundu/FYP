package com.example.fypapplication.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypapplication.R

class FileAttachmentAdapter(
    private val fileUris: List<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<FileAttachmentAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_attachment, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val uri = fileUris[position]
        holder.bind(uri, position)
    }

    override fun getItemCount(): Int = fileUris.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFileName: TextView = itemView.findViewById(R.id.textViewFileName)
        private val imageViewFileType: ImageView = itemView.findViewById(R.id.imageViewFileType)
        private val imageViewRemove: ImageView = itemView.findViewById(R.id.imageViewRemove)

        fun bind(uri: Uri, position: Int) {
            // Get file name from URI
            val fileName = uri.lastPathSegment ?: "Unknown file"
            textViewFileName.text = fileName

            // Set file type icon based on extension
            val fileExtension = fileName.substringAfterLast('.', "").lowercase()
            setFileTypeIcon(fileExtension)

            // Set remove button click listener
            imageViewRemove.setOnClickListener {
                onRemoveClick(position)
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