package com.growguide.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.growguide.app.R
import com.growguide.app.models.LogEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for growth log entries.
 * Displays each entry text with its creation date and edit/delete actions.
 */
class LogAdapter(
    private val logs: List<LogEntry>,
    private val onEdit: (LogEntry) -> Unit,
    private val onDelete: (LogEntry) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val entryText: TextView = itemView.findViewById(R.id.logEntryText)
        val metricsText: TextView = itemView.findViewById(R.id.logMetricsText)
        val dateText: TextView = itemView.findViewById(R.id.logDateText)
        val editButton: ImageButton = itemView.findViewById(R.id.logEditButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.logDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.entryText.text = log.entry

        val metrics = buildString {
            if (log.heightCm > 0) append("${log.heightCm} cm")
            if (log.leafCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("${log.leafCount} leaves")
            }
        }
        if (metrics.isNotEmpty()) {
            holder.metricsText.text = metrics
            holder.metricsText.visibility = android.view.View.VISIBLE
        } else {
            holder.metricsText.visibility = android.view.View.GONE
        }

        val date = log.createdAt?.toDate()
        if (date != null) {
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            holder.dateText.text = formatter.format(date)
        }

        holder.editButton.setOnClickListener { onEdit(log) }
        holder.deleteButton.setOnClickListener { onDelete(log) }
    }

    override fun getItemCount(): Int = logs.size
}
