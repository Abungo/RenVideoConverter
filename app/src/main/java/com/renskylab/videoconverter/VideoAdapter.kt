package com.renskylab.videoconverter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoAdapter(
    private val onItemClick: (File) -> Unit,
    private val onActionClick: (File, View) -> Unit,
    private val onLongClick: (File, View) -> Unit
) : ListAdapter<File, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var isGridLayout: Boolean = false

    override fun getItemViewType(position: Int): Int {
        return if (isGridLayout) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val layout = if (viewType == 1) R.layout.item_video_grid else R.layout.item_video
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val nameText: TextView = itemView.findViewById(R.id.videoName)
        private val detailsText: TextView = itemView.findViewById(R.id.videoDetails)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.actionButton)

        fun bind(file: File) {
            nameText.text = file.name
            val sizeMb = file.length().toDouble() / (1024 * 1024)
            
            if (isGridLayout) {
                detailsText.text = String.format("%.1f MB", sizeMb)
            } else {
                val dateStr = dateFormat.format(Date(file.lastModified()))
                detailsText.text = String.format("%.1f MB • %s", sizeMb, dateStr)
            }

            Glide.with(itemView.context)
                .load(file)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(thumbnail)

            itemView.setOnClickListener { onItemClick(file) }
            itemView.setOnLongClickListener { 
                onLongClick(file, it)
                true 
            }
            actionButton.setOnClickListener { v -> onActionClick(file, v) }
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean = oldItem.absolutePath == newItem.absolutePath
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean = 
            oldItem.lastModified() == newItem.lastModified() && oldItem.length() == newItem.length()
    }
}
