package com.sn.snfilemanager.feature.files.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.sn.snfilemanager.R
import com.sn.snfilemanager.feature.files.data.RecentFile
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RecentImagemodelAdapter(
    private var recentFiles: List<RecentFile>,
    private val context: Context,
    private val onItemClicked: (RecentFile) -> Unit

) :
    RecyclerView.Adapter<RecentImagemodelAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.recent_image_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = recentFiles[position]
        holder.bind(file, onItemClicked)
    }


    override fun getItemCount(): Int {
        return recentFiles.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(file: RecentFile, onItemClicked: (RecentFile) -> Unit) {

            val imageView = itemView.findViewById<ShapeableImageView>(R.id.ivRecentImageItem)
            val titleView = itemView.findViewById<TextView>(R.id.tvRecentImageTitle)
            val timeView = itemView.findViewById<TextView>(R.id.tvRecentImageTime)
            titleView.text = file.displayName

            Glide.with(itemView.context).load(file.uri).into(imageView)

            itemView.setOnClickListener { onItemClicked(file) }

            timeView.text = formatRecentFileTime(itemView.context, file.dateAdded)

        }
    }

    companion object {
        fun formatRecentFileTime(context: Context, timeInSeconds: Long): String {
            val fileTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInSeconds * 1000), ZoneId.systemDefault())

            val now = LocalDateTime.now()

            val minutesAgo = ChronoUnit.MINUTES.between(fileTime, now)
            val hoursAgo = ChronoUnit.HOURS.between(fileTime, now)

            return when {
                hoursAgo < 1 -> String.format(context.getString(R.string.minutes_ago), minutesAgo)
                hoursAgo < 24 -> String.format(context.getString(R.string.hours_ago), hoursAgo)
                else -> DateTimeFormatter.ofPattern("dd MMM yyyy").format(fileTime)
            }
        }
    }

}